package com.teame.bingo.domain.district.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.entity.District;
import com.teame.bingo.domain.district.repository.DistrictRepository;

@ExtendWith(MockitoExtension.class)
class DistrictServiceTest {

    @Mock
    private DistrictRepository districtRepository;

    @InjectMocks
    private DistrictService districtService;

    @Test
    void getDistrictsMapsRepositoryOrder() {
        given(districtRepository.findAllByOrderByNameEnAsc()).willReturn(List.of(
                District.builder().code("11110").nameEn("Dobong-gu").nameKo("도봉구").build(),
                District.builder().code("11680").nameEn("Gangnam-gu").nameKo("강남구").build()));

        List<DistrictResponse> result = districtService.getDistricts();

        assertThat(result).extracting(DistrictResponse::nameEn).containsExactly("Dobong-gu", "Gangnam-gu");
        assertThat(result.get(0).code()).isEqualTo("11110");
        assertThat(result.get(0).nameKo()).isEqualTo("도봉구");
    }
}
