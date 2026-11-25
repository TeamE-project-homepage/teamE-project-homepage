package com.teame.bingo.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.teame.bingo.domain.activity.dto.SearchHistoryRequest;
import com.teame.bingo.domain.activity.dto.SearchHistoryResponse;
import com.teame.bingo.domain.activity.entity.SearchHistory;
import com.teame.bingo.domain.activity.enums.SearchType;
import com.teame.bingo.domain.activity.repository.SearchHistoryRepository;
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
        "spring.datasource.url=jdbc:h2:mem:search-history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Import(SearchHistoryService.class)
class SearchHistoryServiceTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void savesTextHistoryWithCardAndCreatesMemberFromJwtId() {
        UUID memberId = UUID.randomUUID();
        Item item = item();
        tem.flush();
        tem.clear();

        SearchHistoryResponse result = searchHistoryService.saveSearchHistory(memberId,
                new SearchHistoryRequest(SearchType.TEXT, "  plastic bottle  ", item.getId()));
        tem.flush();
        tem.clear();

        SearchHistory stored = searchHistoryRepository.findById(result.historyId()).orElseThrow();
        assertThat(stored.getMember().getId()).isEqualTo(memberId);
        assertThat(stored.getKeyword()).isEqualTo("plastic bottle");
        assertThat(stored.getType()).isEqualTo(SearchType.TEXT);
        assertThat(result.keyword()).isEqualTo("plastic bottle");
        assertThat(result.resultItem().itemId()).isEqualTo(item.getId());
        assertThat(result.resultItem().name()).isEqualTo("Clear PET Bottle");
        assertThat(result.resultItem().categoryId()).isEqualTo(item.getCategory().getId());
        assertThat(result.resultItem().categoryCode()).isEqualTo("PL");
        assertThat(result.resultItem().categoryName()).isEqualTo("Plastic");
        assertThat(result.searchedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(result.searchedAt().toInstant()).isEqualTo(stored.getCreatedAt());
        assertThat(memberRepository.existsById(memberId)).isTrue();
    }

    @Test
    void savesPhotoHistoryWithoutKeywordAndPreservesExistingMemberProfile() {
        Member owner = tem.persist(Member.builder().id(UUID.randomUUID()).name("Alex").build());
        Item item = item();
        tem.flush();
        tem.clear();

        SearchHistoryResponse result = searchHistoryService.saveSearchHistory(owner.getId(),
                new SearchHistoryRequest(SearchType.PHOTO, null, item.getId()));
        tem.clear();

        SearchHistory stored = searchHistoryRepository.findById(result.historyId()).orElseThrow();
        assertThat(stored.getType()).isEqualTo(SearchType.PHOTO);
        assertThat(stored.getKeyword()).isNull();
        assertThat(stored.getResultItem().getId()).isEqualTo(item.getId());
        assertThat(result.keyword()).isNull();
        assertThat(result.resultItem().itemId()).isEqualTo(item.getId());
        assertThat(memberRepository.findById(owner.getId()).orElseThrow().getName()).isEqualTo("Alex");
    }

    @Test
    void savesTextWithoutResultAndPreservesSeparateRepeatedSearchEvents() {
        UUID memberId = UUID.randomUUID();
        SearchHistoryRequest request = new SearchHistoryRequest(SearchType.TEXT, "a".repeat(100), null);

        SearchHistoryResponse first = searchHistoryService.saveSearchHistory(memberId, request);
        SearchHistoryResponse second = searchHistoryService.saveSearchHistory(memberId, request);

        assertThat(first.historyId()).isNotEqualTo(second.historyId());
        assertThat(first.resultItem()).isNull();
        assertThat(second.resultItem()).isNull();
        assertThat(searchHistoryService.getSearchHistories(memberId, 20)).hasSize(2);
    }

    @ParameterizedTest
    @MethodSource("invalidSearchPayloads")
    void rejectsTypeSpecificInvalidPayloadWithoutCreatingData(SearchHistoryRequest payload) {
        UUID memberId = UUID.randomUUID();

        assertThatThrownBy(() -> searchHistoryService.saveSearchHistory(memberId, payload))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThat(memberRepository.existsById(memberId)).isFalse();
        assertThat(searchHistoryRepository.count()).isZero();
    }

    static Stream<SearchHistoryRequest> invalidSearchPayloads() {
        return Stream.of(
                new SearchHistoryRequest(SearchType.TEXT, null, null),
                new SearchHistoryRequest(SearchType.TEXT, "", null),
                new SearchHistoryRequest(SearchType.TEXT, " \t\n", null),
                new SearchHistoryRequest(SearchType.PHOTO, "bottle", 3L),
                new SearchHistoryRequest(SearchType.PHOTO, "", 3L),
                new SearchHistoryRequest(SearchType.PHOTO, null, null));
    }

    @Test
    void rejectsUnknownResultItemBeforeCreatingMemberOrHistory() {
        UUID memberId = UUID.randomUUID();

        assertThatThrownBy(() -> searchHistoryService.saveSearchHistory(memberId,
                new SearchHistoryRequest(SearchType.PHOTO, null, Long.MAX_VALUE)))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND));

        assertThat(memberRepository.existsById(memberId)).isFalse();
        assertThat(searchHistoryRepository.count()).isZero();
    }

    @Test
    void listsOnlyOwnHistoryNewestFirstIncludingEntriesWithoutResults() {
        Member owner = member();
        SearchHistory newer = history(owner, SearchType.TEXT);
        SearchHistory older = tem.persist(SearchHistory.builder().member(owner)
                .type(SearchType.PHOTO).resultItem(item()).build());
        history(member(), SearchType.TEXT);
        tem.flush();
        Instant newerTime = Instant.parse("2026-11-18T00:45:12Z");
        setCreatedAt(newer, newerTime);
        setCreatedAt(older, Instant.parse("2026-11-17T11:03:31Z"));
        tem.clear();

        List<SearchHistoryResponse> result = searchHistoryService.getSearchHistories(owner.getId(), 20);

        assertThat(result).extracting(SearchHistoryResponse::historyId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getFirst().resultItem()).isNull();
        assertThat(result.getFirst().searchedAt().toInstant()).isEqualTo(newerTime);
        assertThat(result.getFirst().searchedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(result.getLast().keyword()).isNull();
        assertThat(result.getLast().resultItem().name()).isEqualTo("Clear PET Bottle");
        assertThat(searchHistoryService.getSearchHistories(owner.getId(), 1))
                .extracting(SearchHistoryResponse::historyId).containsExactly(newer.getId());
    }

    @Test
    void limitsResultsWithStableIdOrderingWhenTimestampsMatch() {
        Member owner = member();
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 55; i++) {
            SearchHistory entry = history(owner, SearchType.TEXT);
            tem.flush();
            setCreatedAt(entry, Instant.parse("2026-11-18T00:45:12Z"));
            ids.add(entry.getId());
        }
        tem.clear();

        List<Long> expected = ids.reversed();
        assertThat(searchHistoryService.getSearchHistories(owner.getId(), 20))
                .extracting(SearchHistoryResponse::historyId).containsExactlyElementsOf(expected.subList(0, 20));
        assertThat(searchHistoryService.getSearchHistories(owner.getId(), 50))
                .extracting(SearchHistoryResponse::historyId).containsExactlyElementsOf(expected.subList(0, 50));
    }

    @Test
    void fetchesOptionalResultAndCategoryTogetherWithoutFilteringEmptyResults() {
        Member owner = member();
        history(owner, SearchType.TEXT);
        tem.persist(SearchHistory.builder().member(owner).type(SearchType.PHOTO).resultItem(item()).build());
        tem.flush();
        tem.clear();

        List<SearchHistory> result = searchHistoryRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(
                owner.getId(), PageRequest.of(0, 20));

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(entry -> entry.getResultItem() == null);
        Item resultItem = result.stream().map(SearchHistory::getResultItem).filter(java.util.Objects::nonNull)
                .findFirst().orElseThrow();
        assertThat(Hibernate.isInitialized(resultItem)).isTrue();
        assertThat(Hibernate.isInitialized(resultItem.getCategory())).isTrue();
    }

    @Test
    void emptyHistoryLookupDoesNotCreateMember() {
        UUID memberId = UUID.randomUUID();

        assertThat(searchHistoryService.getSearchHistories(memberId, 20)).isEmpty();
        assertThat(memberRepository.existsById(memberId)).isFalse();
    }

    @Test
    void deletesOnlyRequestedEntryOwnedByMember() {
        Member owner = member();
        SearchHistory target = history(owner, SearchType.TEXT);
        SearchHistory remaining = history(owner, SearchType.PHOTO);
        SearchHistory foreign = history(member(), SearchType.TEXT);
        tem.flush();
        tem.clear();

        searchHistoryService.deleteSearchHistory(owner.getId(), target.getId());

        assertThat(searchHistoryRepository.findAll()).extracting(SearchHistory::getId)
                .containsExactlyInAnyOrder(remaining.getId(), foreign.getId());
    }

    @Test
    void missingEntryReturnsHistoryNotFound() {
        assertThatThrownBy(() -> searchHistoryService.deleteSearchHistory(UUID.randomUUID(), 128L))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SEARCH_HISTORY_NOT_FOUND));
    }

    @Test
    void foreignEntryReturnsHistoryNotFoundAndIsPreserved() {
        SearchHistory foreign = history(member(), SearchType.TEXT);
        tem.flush();
        tem.clear();

        assertThatThrownBy(() -> searchHistoryService.deleteSearchHistory(UUID.randomUUID(), foreign.getId()))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SEARCH_HISTORY_NOT_FOUND));

        assertThat(searchHistoryRepository.existsById(foreign.getId())).isTrue();
    }

    @Test
    void repeatedSingleDeleteReturnsHistoryNotFound() {
        Member owner = member();
        SearchHistory target = history(owner, SearchType.TEXT);
        tem.flush();
        tem.clear();

        searchHistoryService.deleteSearchHistory(owner.getId(), target.getId());

        assertThatThrownBy(() -> searchHistoryService.deleteSearchHistory(owner.getId(), target.getId()))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SEARCH_HISTORY_NOT_FOUND));
    }

    @Test
    void clearsTextAndPhotoEntriesOnlyForCurrentMember() {
        Member owner = member();
        history(owner, SearchType.TEXT);
        history(owner, SearchType.PHOTO);
        SearchHistory foreign = history(member(), SearchType.TEXT);
        tem.flush();
        tem.clear();

        searchHistoryService.deleteAllSearchHistories(owner.getId());
        searchHistoryService.deleteAllSearchHistories(owner.getId());

        assertThat(searchHistoryRepository.findAll()).extracting(SearchHistory::getId)
                .containsExactly(foreign.getId());
    }

    @Test
    void clearingWithoutMemberRowSucceedsAndPreservesOtherMembersHistory() {
        SearchHistory foreign = history(member(), SearchType.TEXT);
        tem.flush();
        tem.clear();

        searchHistoryService.deleteAllSearchHistories(UUID.randomUUID());

        assertThat(searchHistoryRepository.findAll()).extracting(SearchHistory::getId)
                .containsExactly(foreign.getId());
    }

    private Member member() {
        return tem.persist(Member.builder().id(UUID.randomUUID()).build());
    }

    private SearchHistory history(Member member, SearchType type) {
        return tem.persist(SearchHistory.builder().member(member).type(type)
                .keyword(type == SearchType.TEXT ? "plastic bottle" : null).build());
    }

    private void setCreatedAt(SearchHistory history, Instant createdAt) {
        tem.getEntityManager().createNativeQuery("update search_history set created_at = :createdAt where history_id = :id")
                .setParameter("createdAt", createdAt).setParameter("id", history.getId()).executeUpdate();
    }

    private Item item() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        return tem.persist(Item.builder().category(category).disposalType(type).itemKey("pet_bottle_clear")
                .name("Clear PET Bottle").recyclable(Recyclable.CONDITIONAL).clipPrompt("a photo of a PET bottle")
                .sourceName("Source").sourceUrl("https://example.com").verifiedAt(LocalDate.of(2026, 11, 18)).build());
    }
}
