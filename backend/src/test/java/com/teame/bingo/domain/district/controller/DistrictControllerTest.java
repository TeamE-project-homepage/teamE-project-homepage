package com.teame.bingo.domain.district.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.district.dto.DistrictResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleAreaResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleDongResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleResponse;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.district.enums.DistrictRuleStatus;
import com.teame.bingo.domain.district.service.DistrictRuleService;
import com.teame.bingo.domain.district.service.DistrictService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.SecurityExceptionHandler;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

@WebMvcTest(DistrictController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class DistrictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistrictService districtService;

    @MockitoBean
    private DistrictRuleService districtRuleService;

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

    @Test
    void returnsDongRulesPubliclyWithExactResponseShape() throws Exception {
        given(districtRuleService.getDistrictRule(14L, 1L)).willReturn(new DistrictRuleResponse(
                14L, "Mapo-gu", 1L, DistrictRuleStatus.AVAILABLE, AreaScope.DONG,
                "Transparent bag", null, "18:00~24:00", "Put PET bottles in a separate bag.",
                "Mapo-gu Office", "https://example.com/rules", LocalDate.of(2026, 10, 30),
                List.of(new DistrictRuleAreaResponse(31L, "Mon, Thu", "18:00~24:00", List.of(
                        new DistrictRuleDongResponse("Gongdeok-dong", "공덕동"),
                        new DistrictRuleDongResponse("Ahyeon-dong", "아현동"))))));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.districtId").value(14))
                .andExpect(jsonPath("$.data.districtName").value("Mapo-gu"))
                .andExpect(jsonPath("$.data.disposalTypeId").value(1))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.areaScope").value("DONG"))
                .andExpect(jsonPath("$.data.method").value("Transparent bag"))
                .andExpect(jsonPath("$.data.day").doesNotExist())
                .andExpect(jsonPath("$.data.time").value("18:00~24:00"))
                .andExpect(jsonPath("$.data.localNotes").value("Put PET bottles in a separate bag."))
                .andExpect(jsonPath("$.data.sourceName").value("Mapo-gu Office"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://example.com/rules"))
                .andExpect(jsonPath("$.data.verifiedAt").value("2026-10-30"))
                .andExpect(jsonPath("$.data.areas[0].areaId").value(31))
                .andExpect(jsonPath("$.data.areas[0].day").value("Mon, Thu"))
                .andExpect(jsonPath("$.data.areas[0].time").value("18:00~24:00"))
                .andExpect(jsonPath("$.data.areas[0].dongs[0].nameEn").value("Gongdeok-dong"))
                .andExpect(jsonPath("$.data.areas[0].dongs[0].nameKo").value("공덕동"))
                .andExpect(jsonPath("$.data.areas[0].dongs[0].dongId").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void omitsMissingRuleAndAreaFieldsForPartialResponse() throws Exception {
        given(districtRuleService.getDistrictRule(14L, 1L)).willReturn(new DistrictRuleResponse(
                14L, "Mapo-gu", 1L, DistrictRuleStatus.PARTIAL, AreaScope.DONG,
                "Transparent bag", null, null, null, null, null, null,
                List.of(new DistrictRuleAreaResponse(31L, "Mon, Thu", null, List.of()))));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIAL"))
                .andExpect(jsonPath("$.data.time").doesNotExist())
                .andExpect(jsonPath("$.data.localNotes").doesNotExist())
                .andExpect(jsonPath("$.data.sourceName").doesNotExist())
                .andExpect(jsonPath("$.data.sourceUrl").doesNotExist())
                .andExpect(jsonPath("$.data.verifiedAt").doesNotExist())
                .andExpect(jsonPath("$.data.areas[0].time").doesNotExist())
                .andExpect(jsonPath("$.data.areas[0].dongs").isEmpty());
    }

    @Test
    void availableRuleOmitsOptionalLocalNotesWhenNoExceptionGuidanceExists() throws Exception {
        given(districtRuleService.getDistrictRule(14L, 1L)).willReturn(new DistrictRuleResponse(
                14L, "Mapo-gu", 1L, DistrictRuleStatus.AVAILABLE, AreaScope.DISTRICT,
                "Transparent bag", "Mon, Thu", "18:00~24:00", null,
                "Mapo-gu Office", "https://example.com/rules", LocalDate.of(2026, 10, 30), List.of()));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.localNotes").doesNotExist())
                .andExpect(jsonPath("$.data.method").value("Transparent bag"))
                .andExpect(jsonPath("$.data.day").value("Mon, Thu"))
                .andExpect(jsonPath("$.data.time").value("18:00~24:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_INFO", "UNSUPPORTED"})
    void emptyStatusesContainOnlyIdentityStatusAndEmptyAreas(String status) throws Exception {
        given(districtRuleService.getDistrictRule(14L, 1L)).willReturn(new DistrictRuleResponse(
                14L, "Mapo-gu", 1L, DistrictRuleStatus.valueOf(status),
                null, null, null, null, null, null, null, null, List.of()));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data.districtId").value(14))
                .andExpect(jsonPath("$.data.districtName").value("Mapo-gu"))
                .andExpect(jsonPath("$.data.disposalTypeId").value(1))
                .andExpect(jsonPath("$.data.status").value(status))
                .andExpect(jsonPath("$.data.areas").isEmpty())
                .andExpect(jsonPath("$.data.areaScope").doesNotExist())
                .andExpect(jsonPath("$.data.method").doesNotExist())
                .andExpect(jsonPath("$.data.day").doesNotExist());
    }

    @Test
    void missingDisposalTypeIdReturnsCommon400() throws Exception {
        mockMvc.perform(get("/api/districts/14/rules"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_400"))
                .andExpect(jsonPath("$.error.message").value("Invalid request."));

        verifyNoInteractions(districtRuleService);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "-1,1", "invalid,1", "9223372036854775808,1",
            "14,0", "14,-1", "14,invalid", "14,9223372036854775808"})
    void invalidParametersReturnCommon400(String districtId, String disposalTypeId) throws Exception {
        mockMvc.perform(get("/api/districts/" + districtId + "/rules").param("disposalTypeId", disposalTypeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(districtRuleService);
    }

    @Test
    void missingDistrictReturnsDomain404() throws Exception {
        given(districtRuleService.getDistrictRule(14L, 1L))
                .willThrow(new CustomException(ErrorCode.DISTRICT_NOT_FOUND));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DISTRICT_404"))
                .andExpect(jsonPath("$.error.message").value("District not found."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unregisteredDisposalTypeReturnsCommon400() throws Exception {
        given(districtRuleService.getDistrictRule(14L, 999L))
                .willThrow(new CustomException(ErrorCode.INVALID_INPUT, "disposalTypeId: does not exist"));

        mockMvc.perform(get("/api/districts/14/rules").param("disposalTypeId", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));
    }
}
