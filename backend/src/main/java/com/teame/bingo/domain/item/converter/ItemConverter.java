package com.teame.bingo.domain.item.converter;

import java.util.List;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.dto.SearchItemResponse;
import com.teame.bingo.domain.item.dto.SearchResponse;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.repository.ItemSearchRow;

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

    public static SearchItemResponse toSearchItemResponse(ItemSearchRow row) {
        return new SearchItemResponse(
                row.getItemId(),
                row.getName(),
                row.getCategoryId(),
                row.getCategoryCode(),
                row.getCategoryName(),
                row.getMatchedKeyword());
    }

    public static SearchResponse toSearchResponse(String query, List<SearchItemResponse> items) {
        return new SearchResponse(query, items.size(), items);
    }
}
