package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Hashtag;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {

    Optional<Hashtag> findByName(String name);

    // Search hashtag theo tên
    @Query("""
        SELECT h FROM Hashtag h
        WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND (:cursor IS NULL OR h.id < :cursor)
        ORDER BY h.id DESC
    """)
    List<Hashtag> searchByName(@Param("keyword") String keyword,
                               @Param("cursor") UUID cursor,
                               Limit limit);
}
