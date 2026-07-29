package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
    @Modifying
    @Query("DELETE FROM Media m WHERE m.id IN :ids AND m.post.id = :postId")
    void deleteByIdsAndPostId(@Param("ids") List<UUID> ids, @Param("postId") UUID postId);
}
