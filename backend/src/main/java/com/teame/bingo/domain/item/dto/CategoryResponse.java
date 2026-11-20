package com.teame.bingo.domain.item.dto;

import com.teame.bingo.domain.item.entity.Category;

public record CategoryResponse(Long categoryId, String code, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getName());
    }
}
