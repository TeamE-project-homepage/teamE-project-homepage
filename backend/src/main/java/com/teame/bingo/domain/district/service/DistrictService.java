package com.teame.bingo.domain.district.service;

import java.util.List;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistrictService {

    private final DistrictRepository districtRepository;

    public List<DistrictResponse> getDistricts() {
        return districtRepository.findAllByOrderByNameEnAsc().stream()
                .map(DistrictResponse::from)
                .toList();
    }
}
