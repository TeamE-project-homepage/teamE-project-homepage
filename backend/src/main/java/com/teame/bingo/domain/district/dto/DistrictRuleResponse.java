package com.teame.bingo.domain.district.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.district.enums.DistrictRuleStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DistrictRuleResponse(
        Long districtId,
        String districtName,
        Long disposalTypeId,
        DistrictRuleStatus status,
        AreaScope areaScope,
        String method,
        String day,
        String time,
        String localNotes,
        String sourceName,
        String sourceUrl,
        LocalDate verifiedAt,
        List<DistrictRuleAreaResponse> areas) {
}
