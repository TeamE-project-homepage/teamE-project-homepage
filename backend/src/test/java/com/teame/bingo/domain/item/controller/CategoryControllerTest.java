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

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.service.CategoryService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listsCategoriesPublicly() throws Exception {
        given(categoryService.getCategories()).willReturn(List.of(
                new CategoryResponse(1L, "PL", "Plastic"),
                new CategoryResponse(2L, "GL", "Glass")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].code").value("PL"))
                .andExpect(jsonPath("$.data[0].name").value("Plastic"))
                .andExpect(jsonPath("$.data[1].name").value("Glass"));
    }

    @Test
    void returnsEmptyArrayWhenNoCategories() throws Exception {
        given(categoryService.getCategories()).willReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
