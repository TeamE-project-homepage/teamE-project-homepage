package com.teame.bingo.domain.activity.service;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.converter.SearchHistoryConverter;
import com.teame.bingo.domain.activity.dto.SearchHistoryRequest;
import com.teame.bingo.domain.activity.dto.SearchHistoryResponse;
import com.teame.bingo.domain.activity.entity.SearchHistory;
import com.teame.bingo.domain.activity.enums.SearchType;
import com.teame.bingo.domain.activity.repository.SearchHistoryRepository;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.domain.member.repository.MemberRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getSearchHistories(UUID memberId, int limit) {
        return searchHistoryRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId, PageRequest.of(0, limit))
                .stream()
                .map(SearchHistoryConverter::toSearchHistoryResponse)
                .toList();
    }

    public SearchHistoryResponse saveSearchHistory(UUID memberId, SearchHistoryRequest request) {
        validateSearchPayload(request);
        Item resultItem = request.resultItemId() == null ? null
                : itemRepository.findWithCategoryById(request.resultItemId())
                        .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        memberRepository.insertIfAbsent(memberId);
        SearchHistory history = searchHistoryRepository.save(SearchHistory.builder()
                .member(memberRepository.getReferenceById(memberId))
                .type(request.searchType())
                .keyword(request.searchType() == SearchType.TEXT ? request.keyword().trim() : null)
                .resultItem(resultItem)
                .build());
        return SearchHistoryConverter.toSearchHistoryResponse(history);
    }

    public void deleteSearchHistory(UUID memberId, Long historyId) {
        if (searchHistoryRepository.deleteByIdAndMemberId(historyId, memberId) == 0) {
            throw new CustomException(ErrorCode.SEARCH_HISTORY_NOT_FOUND);
        }
    }

    public void deleteAllSearchHistories(UUID memberId) {
        searchHistoryRepository.deleteAllByMemberId(memberId);
    }

    private void validateSearchPayload(SearchHistoryRequest request) {
        if (request.searchType() == SearchType.TEXT
                && (request.keyword() == null || request.keyword().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "keyword: must not be blank for TEXT searches");
        }
        if (request.searchType() == SearchType.PHOTO) {
            if (request.keyword() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "keyword: must be omitted for PHOTO searches");
            }
            if (request.resultItemId() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "resultItemId: is required for PHOTO searches");
            }
        }
    }
}
