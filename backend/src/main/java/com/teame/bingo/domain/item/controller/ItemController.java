package com.teame.bingo.domain.item.controller;

import java.util.List;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Item")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "List all items sorted by category order then name")
    @GetMapping
    public ApiResponse<List<ItemSummaryResponse>> getItems() {
        return ApiResponse.ok(itemService.getItems());
    }

    @Operation(
            summary = "Get an item summary",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Item summary"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "itemId is not a positive number (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "Item not found (ITEM_404)")
            })
    @GetMapping("/{itemId}")
    public ApiResponse<ItemSummaryResponse> getItem(
            @PathVariable @Positive(message = "must be greater than 0") Long itemId) {
        return ApiResponse.ok(itemService.getItem(itemId));
    }
}
