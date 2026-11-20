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

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.service.CategoryService;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ItemService itemService;

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

    @Test
    void listsCategoryItems() throws Exception {
        given(itemService.getItemsByCategory(1L)).willReturn(List.of(
                new ItemSummaryResponse(10L, "PET Bottle", 1L, "PL", "Plastic", 3L)));

        mockMvc.perform(get("/api/categories/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].itemId").value(10))
                .andExpect(jsonPath("$.data[0].name").value("PET Bottle"))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].categoryCode").value("PL"))
                .andExpect(jsonPath("$.data[0].categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data[0].disposalTypeId").value(3));
    }

    @Test
    void returnsEmptyArrayWhenCategoryHasNoItems() throws Exception {
        given(itemService.getItemsByCategory(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/categories/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsNotFoundForUnknownCategory() throws Exception {
        given(itemService.getItemsByCategory(99L))
                .willThrow(new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(get("/api/categories/99/items"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_404"))
                .andExpect(jsonPath("$.error.message").value("Category not found."));
    }

    @Test
    void returnsBadRequestForNonNumericCategoryId() throws Exception {
        mockMvc.perform(get("/api/categories/abc/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void returnsBadRequestForNonPositiveCategoryId(String categoryId) throws Exception {
        mockMvc.perform(get("/api/categories/{categoryId}/items", categoryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_400"))
                .andExpect(jsonPath("$.error.message").value("categoryId: must be greater than 0"));

        verifyNoInteractions(itemService);
    }
}
