package com.teame.bingo.domain.member.repository;

import com.teame.bingo.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    // PostgreSQL handles concurrent first-time requests without replacing an existing profile.
    @Modifying
    @Query(value = """
            INSERT INTO member (member_id, created_at, updated_at)
            VALUES (:memberId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") UUID memberId);
}
