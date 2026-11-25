package com.teame.bingo.domain.activity.dto;

import com.teame.bingo.domain.activity.enums.SearchType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SearchHistoryRequest(
        @NotNull SearchType searchType,
        @Size(max = 100) String keyword,
        @Positive Long resultItemId) {
}
