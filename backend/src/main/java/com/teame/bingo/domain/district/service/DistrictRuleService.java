package com.teame.bingo.domain.district.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.teame.bingo.domain.district.converter.DistrictRuleConverter;
import com.teame.bingo.domain.district.dto.DistrictRuleAreaResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleResponse;
import com.teame.bingo.domain.district.entity.District;
import com.teame.bingo.domain.district.entity.DistrictRule;
import com.teame.bingo.domain.district.entity.DistrictRuleArea;
import com.teame.bingo.domain.district.entity.DistrictRuleAreaDong;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.district.enums.DistrictRuleStatus;
import com.teame.bingo.domain.district.repository.DistrictRepository;
import com.teame.bingo.domain.district.repository.DistrictRuleAreaDongRepository;
import com.teame.bingo.domain.district.repository.DistrictRuleAreaRepository;
import com.teame.bingo.domain.district.repository.DistrictRuleRepository;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.repository.DisposalTypeRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistrictRuleService {

    private static final Set<String> SUPPORTED_DISPOSAL_TYPES = Set.of(
            "RECYCLABLE", "GENERAL", "FOOD", "BULKY", "NON_COMBUSTIBLE");

    private final DistrictRepository districtRepository;
    private final DisposalTypeRepository disposalTypeRepository;
    private final DistrictRuleRepository districtRuleRepository;
    private final DistrictRuleAreaRepository districtRuleAreaRepository;
    private final DistrictRuleAreaDongRepository districtRuleAreaDongRepository;

    public DistrictRuleResponse getDistrictRule(Long districtId, Long disposalTypeId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISTRICT_NOT_FOUND));
        DisposalType disposalType = disposalTypeRepository.findById(disposalTypeId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "disposalTypeId: does not exist"));
        if (!SUPPORTED_DISPOSAL_TYPES.contains(disposalType.getCode())) {
            return DistrictRuleConverter.toEmptyResponse(district, disposalTypeId, DistrictRuleStatus.UNSUPPORTED);
        }

        return districtRuleRepository.findByDistrictIdAndDisposalTypeId(districtId, disposalTypeId)
                .map(rule -> {
                    List<DistrictRuleAreaResponse> areas = getAreas(rule);
                    DistrictRuleStatus status = isComplete(rule, areas)
                            ? DistrictRuleStatus.AVAILABLE : DistrictRuleStatus.PARTIAL;
                    return DistrictRuleConverter.toResponse(district, disposalTypeId, rule, status, areas);
                })
                .orElseGet(() -> DistrictRuleConverter.toEmptyResponse(
                        district, disposalTypeId, DistrictRuleStatus.NO_INFO));
    }

    private List<DistrictRuleAreaResponse> getAreas(DistrictRule rule) {
        if (rule.getAreaScope() != AreaScope.DONG) {
            return List.of();
        }
        List<DistrictRuleArea> areas = districtRuleAreaRepository.findAllByDistrictRuleIdOrderBySortOrderAscIdAsc(rule.getId());
        if (areas.isEmpty()) {
            return List.of();
        }
        List<Long> areaIds = areas.stream().map(DistrictRuleArea::getId).toList();
        Map<Long, List<DistrictRuleAreaDong>> dongsByArea = districtRuleAreaDongRepository
                .findAllByDistrictRuleAreaIdInOrderByNameEnAscIdAsc(areaIds).stream()
                .collect(Collectors.groupingBy(dong -> dong.getDistrictRuleArea().getId()));
        return areas.stream()
                .map(area -> DistrictRuleConverter.toAreaResponse(
                        area, rule.getDisposalTime(), dongsByArea.getOrDefault(area.getId(), List.of())))
                .toList();
    }

    private boolean isComplete(DistrictRule rule, List<DistrictRuleAreaResponse> areas) {
        if (!StringUtils.hasText(rule.getMethod()) || !StringUtils.hasText(rule.getLocalNotes())
                || !StringUtils.hasText(rule.getSourceName()) || !StringUtils.hasText(rule.getSourceUrl())
                || rule.getVerifiedAt() == null) {
            return false;
        }
        if (rule.getAreaScope() == AreaScope.DONG) {
            return !areas.isEmpty() && areas.stream().allMatch(area ->
                    StringUtils.hasText(area.day()) && StringUtils.hasText(area.time())
                            && !area.dongs().isEmpty() && area.dongs().stream().allMatch(dong ->
                                    StringUtils.hasText(dong.nameEn()) && StringUtils.hasText(dong.nameKo())));
        }
        if (rule.getAreaScope() == AreaScope.ADDRESS) {
            return StringUtils.hasText(rule.getDisposalTime());
        }
        return rule.getAreaScope() == AreaScope.DISTRICT
                && StringUtils.hasText(rule.getDisposalDay()) && StringUtils.hasText(rule.getDisposalTime());
    }
}
