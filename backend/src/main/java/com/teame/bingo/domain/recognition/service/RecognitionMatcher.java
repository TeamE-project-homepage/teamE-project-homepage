package com.teame.bingo.domain.recognition.service;

import com.teame.bingo.domain.item.converter.ItemConverter;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.repository.CategoryRepository;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.domain.item.service.ItemService;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecognitionMatcher {

    private static final Map<String, String> CATEGORY_CODES = Map.of(
            "plastic", "PL",
            "vinyl", "VN",
            "paper", "PA",
            "glass", "GL",
            "can", "ME",
            "appliance", "AP",
            "furniture", "FU",
            "clothing", "CL",
            "food", "FO",
            "large_waste", "BK");

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemService itemService;

    @Value("${app.recognition.min-confidence}")
    private double minConfidence;

    public RecognitionMatchResult match(String label, double score) {
        if (label == null || score < minConfidence) {
            return RecognitionMatchResult.unmatched(label, score);
        }
        List<ItemSummaryResponse> candidates = itemRepository.findWithCategoryByItemKey(label)
                .map(ItemConverter::toItemSummaryResponse)
                .map(List::of)
                .orElseGet(() -> categoryItems(label));
        if (candidates.isEmpty()) {
            return RecognitionMatchResult.unmatched(label, score);
        }
        return RecognitionMatchResult.matched(label, score, candidates);
    }

    private List<ItemSummaryResponse> categoryItems(String label) {
        String code = CATEGORY_CODES.get(label);
        if (code == null) {
            return List.of();
        }
        return categoryRepository.findByCode(code)
                .map(category -> itemService.getItemsByCategory(category.getId()))
                .orElse(List.of());
    }
}
