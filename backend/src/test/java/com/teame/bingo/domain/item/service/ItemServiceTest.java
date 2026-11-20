package com.teame.bingo.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.repository.CategoryRepository;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItemsMapsRepositoryOrderToSummaries() {
        Category plastic = Category.builder().id(1L).code("PL").name("Plastic").build();
        Category glass = Category.builder().id(2L).code("GL").name("Glass").build();
        DisposalType type = DisposalType.builder().id(3L).code("RECYCLABLE").name("Recyclable Waste").build();
        given(itemRepository.findAllOrderByCategoryAndName()).willReturn(List.of(
                Item.builder().id(11L).category(plastic).disposalType(type).name("Cup").build(),
                Item.builder().id(10L).category(glass).disposalType(type).name("Bottle").build()));

        List<ItemSummaryResponse> result = itemService.getItems();

        assertThat(result).extracting(ItemSummaryResponse::name).containsExactly("Cup", "Bottle");
        assertThat(result.get(0).itemId()).isEqualTo(11L);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).categoryCode()).isEqualTo("PL");
        assertThat(result.get(0).categoryName()).isEqualTo("Plastic");
        assertThat(result.get(0).disposalTypeId()).isEqualTo(3L);
    }

    @Test
    void getItemsReturnsEmptyListWhenNoItems() {
        given(itemRepository.findAllOrderByCategoryAndName()).willReturn(List.of());

        assertThat(itemService.getItems()).isEmpty();
    }

    @Test
    void getItemMapsFoundItemToSummary() {
        Category category = Category.builder().id(1L).code("PL").name("Plastic").build();
        DisposalType type = DisposalType.builder().id(3L).code("RECYCLABLE").name("Recyclable Waste").build();
        given(itemRepository.findWithCategoryById(10L)).willReturn(Optional.of(
                Item.builder().id(10L).category(category).disposalType(type).name("Bottle").build()));

        ItemSummaryResponse result = itemService.getItem(10L);

        assertThat(result.itemId()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Bottle");
        assertThat(result.categoryId()).isEqualTo(1L);
        assertThat(result.categoryCode()).isEqualTo("PL");
        assertThat(result.categoryName()).isEqualTo("Plastic");
        assertThat(result.disposalTypeId()).isEqualTo(3L);
    }

    @Test
    void getItemThrowsWhenItemMissing() {
        given(itemRepository.findWithCategoryById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(99L))
                .isInstanceOfSatisfying(CustomException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("Item not found.");
                });
    }

    @Test
    void getItemsByCategoryMapsRepositoryResult() {
        Category category = Category.builder().id(1L).code("PL").name("Plastic").build();
        DisposalType type = DisposalType.builder().id(3L).code("RECYCLABLE").name("Recyclable Waste").build();
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(itemRepository.findAllByCategoryIdOrderByName(1L)).willReturn(List.of(
                Item.builder().id(10L).category(category).disposalType(type).name("Bottle").build(),
                Item.builder().id(11L).category(category).disposalType(type).name("Cup").build()));

        List<ItemSummaryResponse> result = itemService.getItemsByCategory(1L);

        assertThat(result).extracting(ItemSummaryResponse::name).containsExactly("Bottle", "Cup");
        assertThat(result).extracting(ItemSummaryResponse::itemId).containsExactly(10L, 11L);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).categoryCode()).isEqualTo("PL");
        assertThat(result.get(0).categoryName()).isEqualTo("Plastic");
        assertThat(result.get(0).disposalTypeId()).isEqualTo(3L);
    }

    @Test
    void getItemsByCategoryThrowsWhenCategoryMissing() {
        given(categoryRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> itemService.getItemsByCategory(99L))
                .isInstanceOfSatisfying(CustomException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("Category not found.");
                });
        verifyNoInteractions(itemRepository);
    }

    @Test
    void getItemsByCategoryReturnsEmptyListWhenNoItems() {
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(itemRepository.findAllByCategoryIdOrderByName(1L)).willReturn(List.of());

        assertThat(itemService.getItemsByCategory(1L)).isEmpty();
    }
}
