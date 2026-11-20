package com.teame.bingo.domain.district.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.service.DistrictService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(DistrictController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class DistrictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistrictService districtService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listsDistrictsPublicly() throws Exception {
        given(districtService.getDistricts()).willReturn(List.of(
                new DistrictResponse(2L, "11110", "Dobong-gu", "도봉구"),
                new DistrictResponse(1L, "11680", "Gangnam-gu", "강남구")));

        mockMvc.perform(get("/api/districts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].districtId").value(2))
                .andExpect(jsonPath("$.data[0].code").value("11110"))
                .andExpect(jsonPath("$.data[0].nameEn").value("Dobong-gu"))
                .andExpect(jsonPath("$.data[0].nameKo").value("도봉구"))
                .andExpect(jsonPath("$.data[1].nameEn").value("Gangnam-gu"));
    }
}
