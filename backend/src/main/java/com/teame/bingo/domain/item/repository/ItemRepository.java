package com.teame.bingo.domain.item.repository;

import java.util.List;
import java.util.Optional;

import com.teame.bingo.domain.item.entity.Item;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @EntityGraph(attributePaths = "category")
    Optional<Item> findWithCategoryById(Long id);

    @Query("select i from Item i join fetch i.category c order by c.sortOrder, c.id, lower(i.name), i.id")
    List<Item> findAllOrderByCategoryAndName();

    @Query("select i from Item i join fetch i.category where i.category.id = :categoryId order by lower(i.name), i.id")
    List<Item> findAllByCategoryIdOrderByName(@Param("categoryId") Long categoryId);
}
