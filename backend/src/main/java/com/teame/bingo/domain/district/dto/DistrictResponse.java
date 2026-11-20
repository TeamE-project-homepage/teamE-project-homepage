package com.teame.bingo.domain.district.dto;

import com.teame.bingo.domain.district.entity.District;

public record DistrictResponse(Long districtId, String code, String nameEn, String nameKo) {

    public static DistrictResponse from(District district) {
        return new DistrictResponse(district.getId(), district.getCode(), district.getNameEn(), district.getNameKo());
    }
}
