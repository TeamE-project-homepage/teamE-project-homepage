package com.teame.bingo.domain.item.repository;

import com.teame.bingo.domain.item.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
