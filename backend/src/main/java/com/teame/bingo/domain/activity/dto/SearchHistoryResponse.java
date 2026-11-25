package com.teame.bingo.domain.activity.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teame.bingo.domain.activity.enums.SearchType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchHistoryResponse(
        Long historyId,
        SearchType searchType,
        String keyword,
        SearchHistoryItemResponse resultItem,
        OffsetDateTime searchedAt) {
}
