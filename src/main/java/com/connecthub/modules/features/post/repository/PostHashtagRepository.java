package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.PostHashtag;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtag.PostHashtagId> {

    @Query("SELECT ph.hashtag.id FROM PostHashtag ph WHERE ph.post.id = :postId")
    Set<UUID> findHashtagIdsByPostId(@Param("postId") UUID postId);

    @Query("""
    SELECT p.id FROM PostHashtag ph
    JOIN ph.post p
    WHERE ph.hashtag.id = :hashtagId
      AND p.isDeleted = false
      AND (:cursor IS NULL OR p.id < :cursor)
    ORDER BY p.id DESC
""")
    List<UUID> findPostIdsByHashtagId(@Param("hashtagId") UUID hashtagId,
                                      @Param("cursor") UUID cursor,
                                      Limit limit);
    @Modifying
    @Query("DELETE FROM PostHashtag ph WHERE ph.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}