package com.teame.bingo.domain.activity.dto;

public record SearchHistoryItemResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryCode,
        String categoryName) {
}
