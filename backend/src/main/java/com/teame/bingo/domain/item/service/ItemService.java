package com.teame.bingo.domain.item.service;

import java.util.List;

import com.teame.bingo.domain.item.converter.ItemConverter;
import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
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

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public List<ItemSummaryResponse> getItems() {
        return itemRepository.findAllOrderByCategoryAndName().stream()
                .map(ItemConverter::toItemSummaryResponse)
                .toList();
    }

    public List<ItemSummaryResponse> getItemsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return itemRepository.findAllByCategoryIdOrderByName(categoryId).stream()
                .map(ItemConverter::toItemSummaryResponse)
                .toList();
    }
}
