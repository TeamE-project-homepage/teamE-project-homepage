package com.teame.bingo.domain.activity.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.activity.dto.SavedGuideItemResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideStatusResponse;
import com.teame.bingo.domain.activity.service.SavedGuideService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(SavedGuideController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class SavedGuideControllerTest {

    private static final UUID MEMBER_ID = UUID.fromString("71ee995b-dc83-4c26-83fd-a02f568dc568");
    private static final String BASE_PATH = "/api/me/saved-guides";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SavedGuideService savedGuideService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void savesGuideForJwtSubject() throws Exception {
        given(savedGuideService.saveGuide(MEMBER_ID, 3L)).willReturn(new SavedGuideStatusResponse(3L, true));

        mockMvc.perform(put(BASE_PATH + "/3")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":{\"itemId\":3,\"saved\":true}}"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(savedGuideService).saveGuide(MEMBER_ID, 3L);
    }

    @Test
    void listsGuideCardsWithSeoulOffset() throws Exception {
        given(savedGuideService.getSavedGuides(MEMBER_ID)).willReturn(List.of(new SavedGuideResponse(
                41L, OffsetDateTime.parse("2026-11-18T09:41:07+09:00"),
                new SavedGuideItemResponse(3L, "Clear PET Bottle", 1L, "PL", "Plastic"))));

        mockMvc.perform(get(BASE_PATH)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].savedGuideId").value(41))
                .andExpect(jsonPath("$.data[0].savedAt").value("2026-11-18T09:41:07+09:00"))
                .andExpect(jsonPath("$.data[0].item.itemId").value(3))
                .andExpect(jsonPath("$.data[0].item.name").value("Clear PET Bottle"))
                .andExpect(jsonPath("$.data[0].item.categoryId").value(1))
                .andExpect(jsonPath("$.data[0].item.categoryCode").value("PL"))
                .andExpect(jsonPath("$.data[0].item.categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data[0].item.disposalTypeId").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void returnsEmptyArrayWhenNothingIsSaved() throws Exception {
        given(savedGuideService.getSavedGuides(MEMBER_ID)).willReturn(List.of());

        mockMvc.perform(get(BASE_PATH)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":[]}"));
    }

    @Test
    void deletesGuideAndReturnsUnsavedState() throws Exception {
        given(savedGuideService.deleteSavedGuide(MEMBER_ID, 3L))
                .willReturn(new SavedGuideStatusResponse(3L, false));

        mockMvc.perform(delete(BASE_PATH + "/3")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":{\"itemId\":3,\"saved\":false}}"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(savedGuideService).deleteSavedGuide(MEMBER_ID, 3L);
    }

    @Test
    void returnsItemNotFoundWhenSavingUnknownItem() throws Exception {
        given(savedGuideService.saveGuide(MEMBER_ID, 3L))
                .willThrow(new CustomException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(put(BASE_PATH + "/3")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ITEM_404"))
                .andExpect(jsonPath("$.error.message").value("Item not found."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/me/saved-guides", "PUT,/api/me/saved-guides/3", "DELETE,/api/me/saved-guides/3"})
    void rejectsMissingToken(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(savedGuideService);
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/me/saved-guides", "PUT,/api/me/saved-guides/3", "DELETE,/api/me/saved-guides/3"})
    void rejectsInvalidToken(String method, String path) throws Exception {
        given(jwtDecoder.decode("invalid-token")).willThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(savedGuideService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "9223372036854775808"})
    void rejectsInvalidItemIdForBothWrites(String itemId) throws Exception {
        for (HttpMethod method : List.of(HttpMethod.PUT, HttpMethod.DELETE)) {
            mockMvc.perform(request(method, BASE_PATH + "/" + itemId)
                            .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("COMMON_400"));
        }

        verifyNoInteractions(savedGuideService);
    }
}
