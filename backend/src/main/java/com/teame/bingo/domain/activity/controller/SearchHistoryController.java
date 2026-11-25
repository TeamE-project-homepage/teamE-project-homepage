package com.teame.bingo.domain.activity.controller;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.dto.SearchHistoryRequest;
import com.teame.bingo.domain.activity.dto.SearchHistoryResponse;
import com.teame.bingo.domain.activity.service.SearchHistoryService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/me/search-histories")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @Operation(
            summary = "Create a search history entry when the user opens a result",
            description = "TEXT requires a keyword (up to 100 characters); resultItemId is optional. "
                    + "PHOTO requires resultItemId and no keyword. Entries are created only by this explicit request.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201", description = "Search history entry created"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "Invalid search type, keyword, or resultItemId (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "Item not found (ITEM_404)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SearchHistoryResponse> saveSearchHistory(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SearchHistoryRequest request) {
        return ApiResponse.ok(searchHistoryService.saveSearchHistory(UUID.fromString(jwt.getSubject()), request));
    }

    @Operation(
            summary = "List recent searches for the authenticated user, newest first",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Recent searches, or an empty array when none exist"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "limit is outside 1-50 or is not a number (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @GetMapping
    public ApiResponse<List<SearchHistoryResponse>> getSearchHistories(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Maximum number of entries, 1-50", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "must be between 1 and 50")
            @Max(value = 50, message = "must be between 1 and 50") int limit) {
        return ApiResponse.ok(searchHistoryService.getSearchHistories(UUID.fromString(jwt.getSubject()), limit));
    }

    @Operation(
            summary = "Delete one search history entry",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Search history entry deleted"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "historyId is not a positive number (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "Entry does not exist or belongs to another user (HISTORY_404)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @DeleteMapping("/{historyId}")
    public ApiResponse<Void> deleteSearchHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") Long historyId) {
        searchHistoryService.deleteSearchHistory(UUID.fromString(jwt.getSubject()), historyId);
        return ApiResponse.ok();
    }

    @Operation(
            summary = "Clear all search history for the authenticated user",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Search history cleared, including when already empty"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @DeleteMapping
    public ApiResponse<Void> deleteAllSearchHistories(@AuthenticationPrincipal Jwt jwt) {
        searchHistoryService.deleteAllSearchHistories(UUID.fromString(jwt.getSubject()));
        return ApiResponse.ok();
    }
}
