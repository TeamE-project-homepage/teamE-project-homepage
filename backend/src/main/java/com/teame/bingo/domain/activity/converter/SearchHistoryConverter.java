package com.teame.bingo.domain.activity.converter;

import java.time.ZoneOffset;

import com.teame.bingo.domain.activity.dto.SearchHistoryItemResponse;
import com.teame.bingo.domain.activity.dto.SearchHistoryResponse;
import com.teame.bingo.domain.activity.entity.SearchHistory;
import com.teame.bingo.domain.activity.enums.SearchType;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.Item;

public final class SearchHistoryConverter {

    private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

    private SearchHistoryConverter() {
    }

    public static SearchHistoryResponse toSearchHistoryResponse(SearchHistory history) {
        Item item = history.getResultItem();
        SearchHistoryItemResponse resultItem = null;
        if (item != null) {
            Category category = item.getCategory();
            resultItem = new SearchHistoryItemResponse(
                    item.getId(), item.getName(), category.getId(), category.getCode(), category.getName());
        }
        return new SearchHistoryResponse(
                history.getId(), history.getType(),
                history.getType() == SearchType.TEXT ? history.getKeyword() : null,
                resultItem, history.getCreatedAt().atOffset(SEOUL_OFFSET));
    }
}
