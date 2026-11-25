package com.teame.bingo.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.teame.bingo.domain.activity.dto.SavedGuideResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideStatusResponse;
import com.teame.bingo.domain.activity.entity.SavedGuide;
import com.teame.bingo.domain.activity.repository.SavedGuideRepository;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.enums.Recyclable;
import com.teame.bingo.domain.member.entity.Member;
import com.teame.bingo.domain.member.repository.MemberRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.support.RepositoryTest;

@RepositoryTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${SAVED_GUIDE_TEST_DB_URL:jdbc:h2:mem:saved-guides;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${SAVED_GUIDE_TEST_DB_USERNAME:sa}",
        "spring.datasource.password=${SAVED_GUIDE_TEST_DB_PASSWORD:}"
})
@Import(SavedGuideService.class)
class SavedGuideServiceTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private SavedGuideRepository savedGuideRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SavedGuideService savedGuideService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Category category;

    @Test
    void firstSaveCreatesMemberAndGuide() {
        UUID memberId = UUID.randomUUID();
        Item item = item("Bottle");
        tem.flush();
        tem.clear();

        SavedGuideStatusResponse result = savedGuideService.saveGuide(memberId, item.getId());

        assertThat(result).isEqualTo(new SavedGuideStatusResponse(item.getId(), true));
        assertThat(memberRepository.existsById(memberId)).isTrue();
        assertThat(savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId))
                .extracting(guide -> guide.getItem().getId()).containsExactly(item.getId());
    }

    @Test
    void repeatedSavePreservesOriginalRowTimestampAndMemberProfile() {
        Member owner = tem.persist(Member.builder().id(UUID.randomUUID()).name("Alex").build());
        Item item = item("Bottle");
        tem.flush();
        tem.clear();
        savedGuideService.saveGuide(owner.getId(), item.getId());
        SavedGuide original = savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(owner.getId()).getFirst();

        savedGuideService.saveGuide(owner.getId(), item.getId());
        tem.clear();

        List<SavedGuide> saved = savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(owner.getId());
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getId()).isEqualTo(original.getId());
        assertThat(saved.getFirst().getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(memberRepository.findById(owner.getId()).orElseThrow().getName()).isEqualTo("Alex");
    }

    @Test
    void missingItemReturns404WithoutCreatingMember() {
        UUID memberId = UUID.randomUUID();

        assertThatThrownBy(() -> savedGuideService.saveGuide(memberId, Long.MAX_VALUE))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND));

        assertThat(memberRepository.existsById(memberId)).isFalse();
    }

    @Test
    void listsOnlyOwnGuidesNewestFirstAndMapsCardWithSeoulTime() {
        Member owner = member();
        SavedGuide first = guide(owner, item("Bottle"));
        SavedGuide second = guide(owner, item("Box"));
        guide(member(), item("Foreign"));
        tem.flush();
        Instant older = Instant.parse("2026-11-17T11:03:31Z");
        Instant newer = Instant.parse("2026-11-18T00:41:07Z");
        setCreatedAt(first, newer);
        setCreatedAt(second, older);
        tem.clear();

        List<SavedGuideResponse> result = savedGuideService.getSavedGuides(owner.getId());

        assertThat(result).extracting(SavedGuideResponse::savedGuideId)
                .containsExactly(first.getId(), second.getId());
        SavedGuideResponse latest = result.getFirst();
        assertThat(latest.savedAt().toInstant()).isEqualTo(newer);
        assertThat(latest.savedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(latest.item().itemId()).isEqualTo(first.getItem().getId());
        assertThat(latest.item().name()).isEqualTo("Bottle");
        assertThat(latest.item().categoryId()).isEqualTo(first.getItem().getCategory().getId());
        assertThat(latest.item().categoryCode()).isEqualTo("PL");
        assertThat(latest.item().categoryName()).isEqualTo("Plastic");
    }

    @Test
    void usesIdDescendingWhenSaveTimesMatchAndFetchesCardAssociations() {
        Member owner = member();
        SavedGuide first = guide(owner, item("Bottle"));
        SavedGuide second = guide(owner, item("Box"));
        tem.flush();
        Instant sameTime = Instant.parse("2026-11-18T00:41:07Z");
        setCreatedAt(first, sameTime);
        setCreatedAt(second, sameTime);
        tem.clear();

        List<SavedGuide> result = savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(owner.getId());

        assertThat(result).extracting(SavedGuide::getId).containsExactly(second.getId(), first.getId());
        for (SavedGuide saved : result) {
            assertThat(Hibernate.isInitialized(saved.getItem())).isTrue();
            assertThat(Hibernate.isInitialized(saved.getItem().getCategory())).isTrue();
        }
    }

    @Test
    void listsEmptyWithoutCreatingMember() {
        UUID memberId = UUID.randomUUID();

        assertThat(savedGuideService.getSavedGuides(memberId)).isEmpty();
        assertThat(memberRepository.existsById(memberId)).isFalse();
    }

    @Test
    void deletesOnlyOwnGuideAndRepeatedDeletionSucceeds() {
        Member owner = member();
        Item target = item("Bottle");
        guide(owner, target);
        SavedGuide ownRemaining = guide(owner, item("Box"));
        SavedGuide foreign = guide(member(), target);
        tem.flush();
        tem.clear();

        assertThat(savedGuideService.deleteSavedGuide(owner.getId(), target.getId()))
                .isEqualTo(new SavedGuideStatusResponse(target.getId(), false));
        assertThat(savedGuideService.deleteSavedGuide(owner.getId(), target.getId()).saved()).isFalse();

        assertThat(savedGuideRepository.findAll()).extracting(SavedGuide::getId)
                .containsExactlyInAnyOrder(ownRemaining.getId(), foreign.getId());
    }

    @Test
    void deletingMissingItemAndMemberSucceedsWithoutCreatingMember() {
        UUID memberId = UUID.randomUUID();

        assertThat(savedGuideService.deleteSavedGuide(memberId, Long.MAX_VALUE))
                .isEqualTo(new SavedGuideStatusResponse(Long.MAX_VALUE, false));
        assertThat(memberRepository.existsById(memberId)).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFirstSavesCreateOneMemberAndOneGuide() throws Exception {
        UUID memberId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long itemId = transaction.execute(status -> {
            Item item = item("Concurrent");
            tem.flush();
            return item.getId();
        });
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var requests = List.of(1, 2).stream().map(index -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent saves");
                }
                return savedGuideService.saveGuide(memberId, itemId);
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var result : requests) {
                assertThat(result.get(10, TimeUnit.SECONDS))
                        .isEqualTo(new SavedGuideStatusResponse(itemId, true));
            }

            transaction.executeWithoutResult(status -> {
                assertThat(memberRepository.existsById(memberId)).isTrue();
                assertThat(savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId)).hasSize(1);
            });
        } finally {
            start.countDown();
            transaction.executeWithoutResult(status -> {
                savedGuideRepository.deleteByMemberIdAndItemId(memberId, itemId);
                memberRepository.deleteById(memberId);
                Item item = tem.find(Item.class, itemId);
                Category category = item.getCategory();
                DisposalType type = item.getDisposalType();
                tem.remove(item);
                tem.flush();
                tem.remove(category);
                tem.remove(type);
            });
        }
    }

    private void setCreatedAt(SavedGuide guide, Instant createdAt) {
        tem.getEntityManager().createNativeQuery("update saved_guide set created_at = :createdAt where saved_guide_id = :id")
                .setParameter("createdAt", createdAt).setParameter("id", guide.getId()).executeUpdate();
    }

    private Member member() {
        return tem.persist(Member.builder().id(UUID.randomUUID()).build());
    }

    private SavedGuide guide(Member member, Item item) {
        return tem.persist(SavedGuide.builder().member(member).item(item).build());
    }

    private Item item(String name) {
        String key = UUID.randomUUID().toString();
        if (category == null) {
            category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        }
        DisposalType type = tem.persist(DisposalType.builder().code("R_" + key.substring(0, 8)).name("Recyclable Waste").build());
        return tem.persist(Item.builder().category(category).disposalType(type).itemKey(key)
                .name(name).recyclable(Recyclable.CONDITIONAL).clipPrompt("a photo of " + key)
                .sourceName("Source").sourceUrl("https://example.com").verifiedAt(LocalDate.of(2026, 11, 18)).build());
    }
}
