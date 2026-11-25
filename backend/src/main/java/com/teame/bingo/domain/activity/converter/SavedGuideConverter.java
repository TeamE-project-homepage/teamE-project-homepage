package com.teame.bingo.domain.activity.converter;

import java.time.ZoneOffset;

import com.teame.bingo.domain.activity.dto.SavedGuideItemResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideResponse;
import com.teame.bingo.domain.activity.entity.SavedGuide;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.Item;

public final class SavedGuideConverter {

    private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

    private SavedGuideConverter() {
    }

    public static SavedGuideResponse toSavedGuideResponse(SavedGuide savedGuide) {
        Item item = savedGuide.getItem();
        Category category = item.getCategory();
        return new SavedGuideResponse(
                savedGuide.getId(),
                savedGuide.getCreatedAt().atOffset(SEOUL_OFFSET),
                new SavedGuideItemResponse(
                        item.getId(), item.getName(), category.getId(), category.getCode(), category.getName()));
    }
}
