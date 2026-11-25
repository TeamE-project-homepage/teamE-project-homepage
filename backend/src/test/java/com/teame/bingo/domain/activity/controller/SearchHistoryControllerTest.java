package com.teame.bingo.domain.activity.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.activity.dto.SearchHistoryItemResponse;
import com.teame.bingo.domain.activity.dto.SearchHistoryRequest;
import com.teame.bingo.domain.activity.dto.SearchHistoryResponse;
import com.teame.bingo.domain.activity.enums.SearchType;
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
    void savesTextHistoryWithResultCard() throws Exception {
        SearchHistoryRequest payload = new SearchHistoryRequest(SearchType.TEXT, "plastic bottle", 3L);
        given(searchHistoryService.saveSearchHistory(MEMBER_ID, payload)).willReturn(new SearchHistoryResponse(
                129L, SearchType.TEXT, "plastic bottle", resultItem(), searchedAt()));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchType\":\"TEXT\",\"keyword\":\"plastic bottle\",\"resultItemId\":3}")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.historyId").value(129))
                .andExpect(jsonPath("$.data.searchType").value("TEXT"))
                .andExpect(jsonPath("$.data.keyword").value("plastic bottle"))
                .andExpect(jsonPath("$.data.resultItem.itemId").value(3))
                .andExpect(jsonPath("$.data.resultItem.name").value("Clear PET Bottle"))
                .andExpect(jsonPath("$.data.resultItem.categoryId").value(1))
                .andExpect(jsonPath("$.data.resultItem.categoryCode").value("PL"))
                .andExpect(jsonPath("$.data.resultItem.categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data.resultItem.disposalTypeId").doesNotExist())
                .andExpect(jsonPath("$.data.searchedAt").value("2026-11-18T09:45:12+09:00"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(searchHistoryService).saveSearchHistory(MEMBER_ID, payload);
    }

    @Test
    void photoHistoryOmitsKeyword() throws Exception {
        SearchHistoryRequest payload = new SearchHistoryRequest(SearchType.PHOTO, null, 3L);
        given(searchHistoryService.saveSearchHistory(MEMBER_ID, payload)).willReturn(new SearchHistoryResponse(
                129L, SearchType.PHOTO, null, resultItem(), searchedAt()));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchType\":\"PHOTO\",\"resultItemId\":3}")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.searchType").value("PHOTO"))
                .andExpect(jsonPath("$.data.keyword").doesNotExist())
                .andExpect(jsonPath("$.data.resultItem.itemId").value(3));
    }

    @Test
    void textHistoryWithoutResultOmitsResultItem() throws Exception {
        SearchHistoryRequest payload = new SearchHistoryRequest(SearchType.TEXT, "cardboard box", null);
        given(searchHistoryService.saveSearchHistory(MEMBER_ID, payload)).willReturn(new SearchHistoryResponse(
                129L, SearchType.TEXT, "cardboard box", null, searchedAt()));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchType\":\"TEXT\",\"keyword\":\"cardboard box\"}")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.keyword").value("cardboard box"))
                .andExpect(jsonPath("$.data.resultItem").doesNotExist());
    }

    @Test
    void listsRecentHistoryWithDefaultLimitAndNullableFieldsOmitted() throws Exception {
        given(searchHistoryService.getSearchHistories(MEMBER_ID, 20)).willReturn(List.of(
                new SearchHistoryResponse(129L, SearchType.TEXT, "cardboard box", null, searchedAt()),
                new SearchHistoryResponse(128L, SearchType.PHOTO, null, resultItem(), searchedAt())));

        mockMvc.perform(get(BASE_PATH)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].historyId").value(129))
                .andExpect(jsonPath("$.data[0].searchType").value("TEXT"))
                .andExpect(jsonPath("$.data[0].keyword").value("cardboard box"))
                .andExpect(jsonPath("$.data[0].resultItem").doesNotExist())
                .andExpect(jsonPath("$.data[0].searchedAt").value("2026-11-18T09:45:12+09:00"))
                .andExpect(jsonPath("$.data[1].searchType").value("PHOTO"))
                .andExpect(jsonPath("$.data[1].keyword").doesNotExist())
                .andExpect(jsonPath("$.data[1].resultItem.itemId").value(3))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(searchHistoryService).getSearchHistories(MEMBER_ID, 20);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void acceptsLimitBoundariesAndReturnsEmptyArray(int limit) throws Exception {
        given(searchHistoryService.getSearchHistories(MEMBER_ID, limit)).willReturn(List.of());

        mockMvc.perform(get(BASE_PATH).param("limit", Integer.toString(limit))
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":[]}"));

        verify(searchHistoryService).getSearchHistories(MEMBER_ID, limit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "51", "not-a-number", "2147483648"})
    void rejectsInvalidLimit(String limit) throws Exception {
        mockMvc.perform(get(BASE_PATH).param("limit", limit)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(searchHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"searchType\":null}", "{\"searchType\":\"OTHER\"}",
            "{\"searchType\":\"TEXT\",\"keyword\":\"bottle\",\"resultItemId\":0}",
            "{\"searchType\":\"TEXT\",\"keyword\":\"bottle\",\"resultItemId\":-1}",
            "{\"searchType\":\"TEXT\",\"keyword\":\"bottle\",\"resultItemId\":\"invalid\"}", "{"})
    void rejectsInvalidRequestBody(String payload) throws Exception {
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(payload)
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(searchHistoryService);
    }

    @Test
    void rejectsKeywordLongerThanDatabaseColumn() throws Exception {
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchType\":\"TEXT\",\"keyword\":\"" + "a".repeat(101) + "\"}")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(searchHistoryService);
    }

    @Test
    void returns404ForUnknownResultItem() throws Exception {
        given(searchHistoryService.saveSearchHistory(MEMBER_ID,
                new SearchHistoryRequest(SearchType.PHOTO, null, 999L)))
                .willThrow(new CustomException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchType\":\"PHOTO\",\"resultItemId\":999}")
                        .with(jwt().jwt(token -> token.subject(MEMBER_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ITEM_404"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void newEndpointsRejectMissingToken(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), BASE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(searchHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void newEndpointsRejectInvalidToken(String method) throws Exception {
        given(jwtDecoder.decode("invalid-token")).willThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(request(HttpMethod.valueOf(method), BASE_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));

        verifyNoInteractions(searchHistoryService);
    }

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

    private SearchHistoryItemResponse resultItem() {
        return new SearchHistoryItemResponse(3L, "Clear PET Bottle", 1L, "PL", "Plastic");
    }

    private OffsetDateTime searchedAt() {
        return OffsetDateTime.parse("2026-11-18T09:45:12+09:00");
    }
}
