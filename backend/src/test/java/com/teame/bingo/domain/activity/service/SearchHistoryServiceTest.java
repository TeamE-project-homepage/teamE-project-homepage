package com.teame.bingo.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.teame.bingo.domain.activity.entity.SearchHistory;
import com.teame.bingo.domain.activity.enums.SearchType;
import com.teame.bingo.domain.activity.repository.SearchHistoryRepository;
import com.teame.bingo.domain.member.entity.Member;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.support.RepositoryTest;

@RepositoryTest
@Import(SearchHistoryService.class)
class SearchHistoryServiceTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SearchHistoryService searchHistoryService;

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
}
