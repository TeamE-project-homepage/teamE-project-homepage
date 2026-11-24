package com.teame.bingo.domain.activity.repository;

import com.teame.bingo.domain.activity.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
}
