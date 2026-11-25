package com.teame.bingo.domain.activity.dto;

public record SavedGuideItemResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryCode,
        String categoryName) {
}
