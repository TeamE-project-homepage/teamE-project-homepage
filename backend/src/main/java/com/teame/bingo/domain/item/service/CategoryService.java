package com.teame.bingo.domain.item.service;

import java.util.List;

import com.teame.bingo.domain.item.dto.CategoryResponse;
import com.teame.bingo.domain.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
