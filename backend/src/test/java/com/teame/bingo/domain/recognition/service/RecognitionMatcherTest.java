package com.teame.bingo.domain.recognition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.enums.Recyclable;
import com.teame.bingo.domain.item.repository.CategoryRepository;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;

@ExtendWith(MockitoExtension.class)
class RecognitionMatcherTest {

    @Mock
    ItemRepository itemRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ItemService itemService;

    @InjectMocks
    RecognitionMatcher matcher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(matcher, "minConfidence", 0.5);
    }

    private Category category(long id, String code) {
        return Category.builder().id(id).code(code).name("Plastic").sortOrder(1).build();
    }

    private Item item(long id, String key, Category category) {
        DisposalType type = DisposalType.builder().id(2L).code("RECYCLABLE").name("Recyclable Waste").build();
        return Item.builder().id(id).itemKey(key).category(category).disposalType(type).name("Bottle")
                .recyclable(Recyclable.CONDITIONAL).build();
    }

    private ItemSummaryResponse summary(long id) {
        return new ItemSummaryResponse(id, "Bottle", 3L, "PL", "Plastic", 2L);
    }

    @Test
    void belowThresholdIsUnmatchedWithoutQueries() {
        RecognitionMatchResult result = matcher.match("plastic", 0.49);

        assertThat(result.matched()).isFalse();
        assertThat(result.candidates()).isEmpty();
        assertThat(result.label()).isEqualTo("plastic");
        assertThat(result.score()).isEqualTo(0.49);
        verifyNoInteractions(itemRepository, categoryRepository, itemService);
    }

    @Test
    void exactThresholdCountsAsMatch() {
        when(itemRepository.findWithCategoryByItemKey("plastic")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("PL")).thenReturn(Optional.of(category(3L, "PL")));
        when(itemService.getItemsByCategory(3L)).thenReturn(List.of(summary(1)));

        assertThat(matcher.match("plastic", 0.5).matched()).isTrue();
    }

    @Test
    void itemKeyReturnsSingleItem() {
        Item item = item(7, "pet_bottle", category(3L, "PL"));
        when(itemRepository.findWithCategoryByItemKey("pet_bottle")).thenReturn(Optional.of(item));

        RecognitionMatchResult result = matcher.match("pet_bottle", 0.9);

        assertThat(result.matched()).isTrue();
        assertThat(result.candidates()).containsExactly(summary(7));
        verifyNoInteractions(categoryRepository, itemService);
    }

    @Test
    void categoryLabelReturnsAllItemsOfCategory() {
        when(itemRepository.findWithCategoryByItemKey("glass")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("GL")).thenReturn(Optional.of(category(5L, "GL")));
        when(itemService.getItemsByCategory(5L)).thenReturn(List.of(summary(1), summary(2)));

        RecognitionMatchResult result = matcher.match("glass", 0.8);

        assertThat(result.matched()).isTrue();
        assertThat(result.candidates()).extracting(ItemSummaryResponse::itemId).containsExactly(1L, 2L);
    }

    @Test
    void itemKeyTakesPrecedenceOverCategoryLabel() {
        Item item = item(9, "can", category(3L, "ME"));
        when(itemRepository.findWithCategoryByItemKey("can")).thenReturn(Optional.of(item));

        assertThat(matcher.match("can", 0.9).candidates()).hasSize(1);
        verifyNoInteractions(categoryRepository, itemService);
    }

    @Test
    void categoryWithoutItemsIsUnmatched() {
        when(itemRepository.findWithCategoryByItemKey("food")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("FO")).thenReturn(Optional.of(category(8L, "FO")));
        when(itemService.getItemsByCategory(8L)).thenReturn(List.of());

        RecognitionMatchResult result = matcher.match("food", 0.9);

        assertThat(result.matched()).isFalse();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void missingCategoryRowIsUnmatched() {
        when(itemRepository.findWithCategoryByItemKey("large_waste")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("BK")).thenReturn(Optional.empty());

        assertThat(matcher.match("large_waste", 0.9).matched()).isFalse();
    }

    @Test
    void unknownLabelIsUnmatched() {
        when(itemRepository.findWithCategoryByItemKey("spaceship")).thenReturn(Optional.empty());

        RecognitionMatchResult result = matcher.match("spaceship", 0.9);

        assertThat(result.matched()).isFalse();
        assertThat(result.candidates()).isEmpty();
        verifyNoInteractions(categoryRepository, itemService);
    }
}
