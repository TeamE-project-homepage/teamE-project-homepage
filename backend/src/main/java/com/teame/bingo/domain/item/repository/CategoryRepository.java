package com.teame.bingo.domain.item.repository;

import java.util.List;
import java.util.Optional;

import com.teame.bingo.domain.item.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAscIdAsc();

    Optional<Category> findByCode(String code);
}
