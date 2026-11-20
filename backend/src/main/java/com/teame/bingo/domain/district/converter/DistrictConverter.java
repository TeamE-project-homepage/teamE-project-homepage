package com.teame.bingo.domain.district.converter;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.entity.District;

public final class DistrictConverter {

    private DistrictConverter() {
    }

    public static DistrictResponse toDistrictResponse(District district) {
        return new DistrictResponse(district.getId(), district.getCode(), district.getNameEn(), district.getNameKo());
    }
}
