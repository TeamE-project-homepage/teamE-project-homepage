package com.teame.bingo.domain.item.controller;

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

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.config.SecurityConfig;
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
    void returnsEmptyArrayWhenNoItems() throws Exception {
        given(itemService.getItems()).willReturn(List.of());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
