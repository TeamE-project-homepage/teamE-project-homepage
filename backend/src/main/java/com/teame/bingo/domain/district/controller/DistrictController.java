package com.teame.bingo.domain.district.controller;

import java.util.List;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleResponse;
import com.teame.bingo.domain.district.service.DistrictRuleService;
import com.teame.bingo.domain.district.service.DistrictService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "District")
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;
    private final DistrictRuleService districtRuleService;

    @Operation(summary = "List all districts sorted by English name")
    @GetMapping
    public ApiResponse<List<DistrictResponse>> getDistricts() {
        return ApiResponse.ok(districtService.getDistricts());
    }

    @Operation(
            summary = "Get district disposal rules for a disposal type",
            description = "Public endpoint. Existing disposal types without a rule return NO_INFO. "
                    + "AVAILABLE requires method, sourceName, sourceUrl, verifiedAt and scope-specific schedules. "
                    + "localNotes is optional exception guidance and does not affect completeness. "
                    + "DISTRICT requires day and time; DONG requires nonempty areas with days, effective times and dongs; "
                    + "ADDRESS requires time and uses sourceUrl for address lookup. "
                    + "Missing applicable fields return PARTIAL. All five current disposal types are supported; "
                    + "other registered types return UNSUPPORTED. Null fields are omitted.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "AVAILABLE, PARTIAL, NO_INFO, or UNSUPPORTED"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "Missing, invalid, or unregistered disposalTypeId; invalid districtId (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404", description = "District not found (DISTRICT_404)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @GetMapping("/{districtId}/rules")
    public ApiResponse<DistrictRuleResponse> getDistrictRule(
            @PathVariable @Positive(message = "must be greater than 0") Long districtId,
            @RequestParam @Positive(message = "must be greater than 0") Long disposalTypeId) {
        return ApiResponse.ok(districtRuleService.getDistrictRule(districtId, disposalTypeId));
    }
}
