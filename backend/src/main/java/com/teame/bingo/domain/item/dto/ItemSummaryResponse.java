package com.teame.bingo.domain.item.dto;

import com.teame.bingo.domain.item.entity.Item;

public record ItemSummaryResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Long disposalTypeId) {

    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
                item.getId(),
                item.getName(),
                item.getCategory().getId(),
                item.getCategory().getCode(),
                item.getCategory().getName(),
                item.getDisposalType().getId());
    }
}
