package com.teame.bingo.domain.activity.service;

import java.util.UUID;

import com.teame.bingo.domain.activity.repository.SearchHistoryRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    public void deleteSearchHistory(UUID memberId, Long historyId) {
        if (searchHistoryRepository.deleteByIdAndMemberId(historyId, memberId) == 0) {
            throw new CustomException(ErrorCode.SEARCH_HISTORY_NOT_FOUND);
        }
    }

    public void deleteAllSearchHistories(UUID memberId) {
        searchHistoryRepository.deleteAllByMemberId(memberId);
    }
}
