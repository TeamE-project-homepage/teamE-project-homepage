package com.teame.bingo.domain.activity.repository;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.entity.SavedGuide;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedGuideRepository extends JpaRepository<SavedGuide, Long> {

    @EntityGraph(attributePaths = {"item", "item.category"})
    List<SavedGuide> findAllByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId);

    // Keep the original row and saved time when concurrent requests save the same item.
    @Modifying
    @Query(value = """
            INSERT INTO saved_guide (member_id, item_id, created_at, updated_at)
            VALUES (:memberId, :itemId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") UUID memberId, @Param("itemId") Long itemId);

    @Modifying
    @Query("delete from SavedGuide g where g.member.id = :memberId and g.item.id = :itemId")
    int deleteByMemberIdAndItemId(@Param("memberId") UUID memberId, @Param("itemId") Long itemId);
}
