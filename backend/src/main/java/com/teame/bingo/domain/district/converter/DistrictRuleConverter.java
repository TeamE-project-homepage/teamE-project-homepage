package com.teame.bingo.domain.district.converter;

import java.util.List;

import com.teame.bingo.domain.district.dto.DistrictRuleAreaResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleDongResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleResponse;
import com.teame.bingo.domain.district.entity.District;
import com.teame.bingo.domain.district.entity.DistrictRule;
import com.teame.bingo.domain.district.entity.DistrictRuleArea;
import com.teame.bingo.domain.district.entity.DistrictRuleAreaDong;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.district.enums.DistrictRuleStatus;
import org.springframework.util.StringUtils;

public final class DistrictRuleConverter {

    private DistrictRuleConverter() {
    }

    public static DistrictRuleResponse toResponse(
            District district, Long disposalTypeId, DistrictRule rule,
            DistrictRuleStatus status, List<DistrictRuleAreaResponse> areas) {
        return new DistrictRuleResponse(
                district.getId(), district.getNameEn(), disposalTypeId, status, rule.getAreaScope(),
                textOrNull(rule.getMethod()),
                rule.getAreaScope() == AreaScope.DISTRICT ? textOrNull(rule.getDisposalDay()) : null,
                textOrNull(rule.getDisposalTime()), textOrNull(rule.getLocalNotes()),
                textOrNull(rule.getSourceName()), textOrNull(rule.getSourceUrl()), rule.getVerifiedAt(), areas);
    }

    public static DistrictRuleResponse toEmptyResponse(
            District district, Long disposalTypeId, DistrictRuleStatus status) {
        return new DistrictRuleResponse(
                district.getId(), district.getNameEn(), disposalTypeId, status,
                null, null, null, null, null, null, null, null, List.of());
    }

    public static DistrictRuleAreaResponse toAreaResponse(
            DistrictRuleArea area, String districtTime, List<DistrictRuleAreaDong> dongs) {
        String areaTime = StringUtils.hasText(area.getDisposalTime()) ? area.getDisposalTime() : districtTime;
        return new DistrictRuleAreaResponse(
                area.getId(), area.getDisposalDay(), textOrNull(areaTime),
                dongs.stream().map(dong -> new DistrictRuleDongResponse(dong.getNameEn(), dong.getNameKo())).toList());
    }

    private static String textOrNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
