package com.teame.bingo.domain.item.controller;

import java.util.List;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.dto.SearchResponse;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            summary = "Search items by English name or keyword",
            description = "Ordered by exact name, exact keyword, prefix, then pg_trgm similarity.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Search results, empty when nothing matches"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "q is missing or blank (SEARCH_400); "
                                + "q is over 50 characters or limit is outside 1-50 (COMMON_400)")
            })
    @GetMapping("/search")
    public ApiResponse<SearchResponse> search(
            // Optional at binding so a missing q returns SEARCH_400 instead of COMMON_400.
            @Parameter(description = "Search term, 1-50 characters after trimming", example = "Plastic Bottle")
            @RequestParam(required = false) String q,
            @Parameter(description = "Maximum number of results, 1-50", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "must be between 1 and 50")
            @Max(value = 50, message = "must be between 1 and 50") int limit) {
        return ApiResponse.ok(itemService.search(q, limit));
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
