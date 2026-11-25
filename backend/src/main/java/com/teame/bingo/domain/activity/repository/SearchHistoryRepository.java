package com.teame.bingo.domain.activity.repository;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @EntityGraph(attributePaths = {"resultItem", "resultItem.category"})
    List<SearchHistory> findAllByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId, Pageable pageable);

    @Modifying
    @Query("delete from SearchHistory h where h.id = :historyId and h.member.id = :memberId")
    int deleteByIdAndMemberId(@Param("historyId") Long historyId, @Param("memberId") UUID memberId);

    @Modifying
    @Query("delete from SearchHistory h where h.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") UUID memberId);
}
