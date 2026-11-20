package com.teame.bingo.domain.item.converter;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.Item;

public final class ItemConverter {

    private ItemConverter() {
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getName());
    }

    public static ItemSummaryResponse toItemSummaryResponse(Item item) {
        Category category = item.getCategory();
        return new ItemSummaryResponse(
                item.getId(),
                item.getName(),
                category.getId(),
                category.getCode(),
                category.getName(),
                item.getDisposalType().getId());
    }
}
