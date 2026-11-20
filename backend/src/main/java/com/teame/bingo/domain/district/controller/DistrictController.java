package com.teame.bingo.domain.district.controller;

import java.util.List;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.service.DistrictService;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "District")
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;

    @Operation(summary = "List all districts sorted by English name")
    @GetMapping
    public ApiResponse<List<DistrictResponse>> getDistricts() {
        return ApiResponse.ok(districtService.getDistricts());
    }
}
