package com.teame.bingo.domain.item.dto;

public record ItemSummaryResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long disposalTypeId) {
}
