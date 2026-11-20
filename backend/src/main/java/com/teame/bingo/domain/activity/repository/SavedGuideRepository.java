package com.teame.bingo.domain.activity.repository;

import com.teame.bingo.domain.activity.entity.SavedGuide;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedGuideRepository extends JpaRepository<SavedGuide, Long> {
}
