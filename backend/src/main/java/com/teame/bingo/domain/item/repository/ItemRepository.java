package com.teame.bingo.domain.item.repository;

import com.teame.bingo.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
