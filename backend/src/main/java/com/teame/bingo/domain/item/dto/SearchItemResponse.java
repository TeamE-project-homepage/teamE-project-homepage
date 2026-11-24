package com.teame.bingo.domain.item.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record SearchItemResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryCode,
        String categoryName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String matchedKeyword) {
}
