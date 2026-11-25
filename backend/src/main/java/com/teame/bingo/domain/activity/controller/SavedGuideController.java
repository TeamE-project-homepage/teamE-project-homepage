package com.teame.bingo.domain.activity.controller;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.dto.SavedGuideResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideStatusResponse;
import com.teame.bingo.domain.activity.service.SavedGuideService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/me/saved-guides")
@RequiredArgsConstructor
public class SavedGuideController {

    private final SavedGuideService savedGuideService;

    @Operation(
            summary = "List the authenticated user's saved guides, newest first",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Saved guides, or an empty array when none exist"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @GetMapping
    public ApiResponse<List<SavedGuideResponse>> getSavedGuides(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(savedGuideService.getSavedGuides(UUID.fromString(jwt.getSubject())));
    }

    @Operation(
            summary = "Save a guide, returning success even when already saved",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Guide saved; saved is always true"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "itemId is not a positive number (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "Item not found (ITEM_404)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @PutMapping("/{itemId}")
    public ApiResponse<SavedGuideStatusResponse> saveGuide(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") Long itemId) {
        return ApiResponse.ok(savedGuideService.saveGuide(UUID.fromString(jwt.getSubject()), itemId));
    }

    @Operation(
            summary = "Remove a saved guide, returning success even when not saved",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "Guide removed; saved is always false"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "itemId is not a positive number (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401", description = "Authentication is required (COMMON_401)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @DeleteMapping("/{itemId}")
    public ApiResponse<SavedGuideStatusResponse> deleteSavedGuide(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") Long itemId) {
        return ApiResponse.ok(savedGuideService.deleteSavedGuide(UUID.fromString(jwt.getSubject()), itemId));
    }
}
