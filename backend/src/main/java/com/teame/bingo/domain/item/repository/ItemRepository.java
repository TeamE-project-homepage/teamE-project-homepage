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

    /**
     * Searches item names and their keywords (aliases) with pg_trgm. Each item keeps its best match:
     * exact name (0), exact keyword (1), prefix (2), then trigram similarity (3), ties broken by
     * similarity. Postgres only; the pg_trgm extension must be enabled on the database.
     *
     * @param query     lower-cased search term
     * @param prefix    query with LIKE wildcards escaped, followed by %
     * @param threshold minimum similarity() for a fuzzy match
     */
    @Query(value = """
            SELECT best.item_id AS "itemId", best.name AS "name", c.category_id AS "categoryId",
                   c.code AS "categoryCode", c.name AS "categoryName", best.matched_keyword AS "matchedKeyword"
            FROM (
                SELECT DISTINCT ON (m.item_id) m.*
                FROM (
                    SELECT i.item_id, i.name, i.category_id, CAST(NULL AS varchar) AS matched_keyword,
                           CASE WHEN lower(i.name) = :query THEN 0
                                WHEN lower(i.name) LIKE :prefix ESCAPE '\\' THEN 2
                                ELSE 3 END AS match_rank,
                           similarity(lower(i.name), :query) AS score
                    FROM item i
                    WHERE lower(i.name) LIKE :prefix ESCAPE '\\'
                       OR similarity(lower(i.name), :query) >= :threshold
                    UNION ALL
                    SELECT i.item_id, i.name, i.category_id, k.keyword,
                           CASE WHEN lower(k.keyword) = :query THEN 1
                                WHEN lower(k.keyword) LIKE :prefix ESCAPE '\\' THEN 2
                                ELSE 3 END,
                           similarity(lower(k.keyword), :query)
                    FROM item_keyword k
                    JOIN item i ON i.item_id = k.item_id
                    WHERE lower(k.keyword) LIKE :prefix ESCAPE '\\'
                       OR similarity(lower(k.keyword), :query) >= :threshold
                ) m
                ORDER BY m.item_id, m.match_rank, m.score DESC, m.matched_keyword IS NOT NULL
            ) best
            JOIN category c ON c.category_id = best.category_id
            ORDER BY best.match_rank, best.score DESC, lower(best.name), best.item_id
            LIMIT :limit
            """, nativeQuery = true)
    List<ItemSearchRow> search(
            @Param("query") String query,
            @Param("prefix") String prefix,
            @Param("threshold") double threshold,
            @Param("limit") int limit);
}
