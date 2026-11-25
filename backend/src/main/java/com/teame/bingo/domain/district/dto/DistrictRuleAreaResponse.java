package com.teame.bingo.domain.district.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DistrictRuleAreaResponse(Long areaId, String day, String time, List<DistrictRuleDongResponse> dongs) {
}
