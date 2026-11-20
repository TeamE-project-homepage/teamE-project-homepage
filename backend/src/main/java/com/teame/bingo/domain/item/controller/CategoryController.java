package com.teame.bingo.domain.item.controller;

import java.util.List;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.service.CategoryService;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Category")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ItemService itemService;

    @Operation(summary = "List all categories sorted by sort order")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.ok(categoryService.getCategories());
    }

    @Operation(
            summary = "List items in a category sorted by name",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Items in the category"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "Category not found")
            })
    @GetMapping("/{categoryId}/items")
    public ApiResponse<List<ItemSummaryResponse>> getCategoryItems(@PathVariable Long categoryId) {
        return ApiResponse.ok(itemService.getItemsByCategory(categoryId));
    }
}
