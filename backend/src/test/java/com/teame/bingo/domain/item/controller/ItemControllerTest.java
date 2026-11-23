package com.teame.bingo.domain.item.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.dto.SearchItemResponse;
import com.teame.bingo.domain.item.dto.SearchResponse;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(ItemController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listsItemsPublicly() throws Exception {
        given(itemService.getItems()).willReturn(List.of(
                new ItemSummaryResponse(10L, "PET Bottle", 1L, "PL", "Plastic", 3L),
                new ItemSummaryResponse(20L, "Jar", 2L, "GL", "Glass", 4L)));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].itemId").value(10))
                .andExpect(jsonPath("$.data[0].name").value("PET Bottle"))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].categoryCode").value("PL"))
                .andExpect(jsonPath("$.data[0].categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data[0].disposalTypeId").value(3))
                .andExpect(jsonPath("$.data[1].name").value("Jar"));
    }

    @Test
    void returnsItemPublicly() throws Exception {
        given(itemService.getItem(10L)).willReturn(
                new ItemSummaryResponse(10L, "PET Bottle", 1L, "PL", "Plastic", 3L));

        mockMvc.perform(get("/api/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemId").value(10))
                .andExpect(jsonPath("$.data.name").value("PET Bottle"))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.categoryCode").value("PL"))
                .andExpect(jsonPath("$.data.categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data.disposalTypeId").value(3));
    }

    @Test
    void returnsNotFoundForUnknownItem() throws Exception {
        given(itemService.getItem(99L)).willThrow(new CustomException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ITEM_404"))
                .andExpect(jsonPath("$.error.message").value("Item not found."));
    }

    @Test
    void returnsBadRequestForNonNumericItemId() throws Exception {
        mockMvc.perform(get("/api/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void returnsBadRequestForNonPositiveItemId(String itemId) throws Exception {
        mockMvc.perform(get("/api/items/{itemId}", itemId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_400"))
                .andExpect(jsonPath("$.error.message").value("itemId: must be greater than 0"));

        verifyNoInteractions(itemService);
    }

    @Test
    void returnsEmptyArrayWhenNoItems() throws Exception {
        given(itemService.getItems()).willReturn(List.of());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void searchesItemsPubliclyAndOmitsNullMatchedKeyword() throws Exception {
        given(itemService.search("Plastic Bottle", 20)).willReturn(new SearchResponse("Plastic Bottle", 2, List.of(
                new SearchItemResponse(3L, "PET Bottle", 1L, "PL", "Plastic", "plastic bottle"),
                new SearchItemResponse(21L, "Plastic Bottle Cap", 1L, "PL", "Plastic", null))));

        mockMvc.perform(get("/api/items/search").param("q", "Plastic Bottle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.query").value("Plastic Bottle"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].itemId").value(3))
                .andExpect(jsonPath("$.data.items[0].categoryCode").value("PL"))
                .andExpect(jsonPath("$.data.items[0].matchedKeyword").value("plastic bottle"))
                .andExpect(jsonPath("$.data.items[1].matchedKeyword").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "51"})
    void rejectsSearchLimitOutOfRange(String limit) throws Exception {
        mockMvc.perform(get("/api/items/search").param("q", "can").param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"))
                .andExpect(jsonPath("$.error.message").value("limit: must be between 1 and 50"));

        verifyNoInteractions(itemService);
    }
}
