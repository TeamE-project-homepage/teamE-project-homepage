package com.teame.bingo.domain.item.service;

import java.util.List;
import java.util.Locale;

import com.teame.bingo.domain.item.converter.ItemConverter;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.item.dto.SearchItemResponse;
import com.teame.bingo.domain.item.dto.SearchResponse;
import com.teame.bingo.domain.item.repository.CategoryRepository;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private static final int MAX_QUERY_LENGTH = 50;

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public List<ItemSummaryResponse> getItems() {
        return itemRepository.findAllOrderByCategoryAndName().stream()
                .map(ItemConverter::toItemSummaryResponse)
                .toList();
    }

    public ItemSummaryResponse getItem(Long itemId) {
        return itemRepository.findWithCategoryById(itemId)
                .map(ItemConverter::toItemSummaryResponse)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
    }

    public List<ItemSummaryResponse> getItemsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return itemRepository.findAllByCategoryIdOrderByName(categoryId).stream()
                .map(ItemConverter::toItemSummaryResponse)
                .toList();
    }

    public SearchResponse search(String rawQuery, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_REQUIRED);
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "q: size must be between 1 and 50");
        }
        String normalized = query.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String prefix = normalized.replaceAll("([\\\\%_])", "\\\\$1") + "%";

        List<SearchItemResponse> items = itemRepository.search(normalized, prefix, limit)
                .stream()
                .map(ItemConverter::toSearchItemResponse)
                .toList();
        return ItemConverter.toSearchResponse(query, items);
    }
}
