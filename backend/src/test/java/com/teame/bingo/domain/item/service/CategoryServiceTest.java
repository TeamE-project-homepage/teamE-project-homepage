package com.teame.bingo.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getCategoriesMapsRepositoryOrder() {
        given(categoryRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(
                Category.builder().code("PL").name("Plastic").sortOrder(1).build(),
                Category.builder().code("GL").name("Glass").sortOrder(2).build()));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).extracting(CategoryResponse::name).containsExactly("Plastic", "Glass");
        assertThat(result.get(0).code()).isEqualTo("PL");
    }

    @Test
    void getCategoriesReturnsEmptyListWhenNone() {
        given(categoryRepository.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of());

        assertThat(categoryService.getCategories()).isEmpty();
    }
}
