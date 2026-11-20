package com.teame.bingo.domain.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.support.RepositoryTest;

@RepositoryTest
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void ordersBySortOrderThenId() {
        tem.persist(Category.builder().code("C3").name("Third").sortOrder(3).build());
        tem.persist(Category.builder().code("B1").name("TieFirst").sortOrder(1).build());
        tem.persist(Category.builder().code("A0").name("Zero").sortOrder(0).build());
        tem.persist(Category.builder().code("B2").name("TieSecond").sortOrder(1).build());
        tem.flush();
        tem.clear();

        List<Category> result = categoryRepository.findAllByOrderBySortOrderAscIdAsc();

        assertThat(result).extracting(Category::getName)
                .containsExactly("Zero", "TieFirst", "TieSecond", "Third");
    }

    @Test
    void returnsEmptyListWhenNoCategories() {
        assertThat(categoryRepository.findAllByOrderBySortOrderAscIdAsc()).isEmpty();
    }
}
