package com.teame.bingo.domain.activity.controller;

import java.util.UUID;

import com.teame.bingo.domain.activity.service.SearchHistoryService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/me/search-histories")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

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
