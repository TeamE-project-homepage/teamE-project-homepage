package com.teame.bingo.domain.activity.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.activity.service.SearchHistoryService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(SearchHistoryController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class SearchHistoryControllerTest {

    private static final UUID MEMBER_ID = UUID.fromString("71ee995b-dc83-4c26-83fd-a02f568dc568");
    private static final String BASE_PATH = "/api/me/search-histories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchHistoryService searchHistoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void deletesOneEntryForJwtSubject() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/128")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true}"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(searchHistoryService).deleteSearchHistory(MEMBER_ID, 128L);
    }

    @Test
    void clearsAllEntriesForJwtSubject() throws Exception {
        mockMvc.perform(delete(BASE_PATH)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true}"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(searchHistoryService).deleteAllSearchHistories(MEMBER_ID);
    }

    @Test
    void returnsHistoryNotFoundForMissingOrForeignEntry() throws Exception {
        doThrow(new CustomException(ErrorCode.SEARCH_HISTORY_NOT_FOUND))
                .when(searchHistoryService).deleteSearchHistory(MEMBER_ID, 128L);

        mockMvc.perform(delete(BASE_PATH + "/128")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("HISTORY_404"))
                .andExpect(jsonPath("$.error.message").value("Search history not found."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/me/search-histories", "/api/me/search-histories/128"})
    void rejectsMissingToken(String path) throws Exception {
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(searchHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/me/search-histories", "/api/me/search-histories/128"})
    void rejectsInvalidToken(String path) throws Exception {
        given(jwtDecoder.decode("invalid-token")).willThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(searchHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "9223372036854775808"})
    void rejectsInvalidHistoryId(String historyId) throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/" + historyId)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(searchHistoryService);
    }
}
