package com.teame.bingo.domain.item.controller;

import java.util.List;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.service.CategoryService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Category")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List all categories sorted by sort order")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.ok(categoryService.getCategories());
    }
}
