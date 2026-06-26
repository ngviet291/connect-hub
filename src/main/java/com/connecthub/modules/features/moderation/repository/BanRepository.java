package com.connecthub.modules.features.moderation.repository;

import com.connecthub.modules.features.moderation.entity.Ban;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BanRepository extends JpaRepository<Ban, UUID> {
    Page<Ban> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT b FROM Ban b WHERE b.user.id = :userId " +
            "AND (b.endDate IS NULL OR b.endDate > :now)")
    Optional<Ban> findActiveBanByUserId(
            @Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Ban b " +
            "WHERE b.user.id = :userId AND (b.endDate IS NULL OR b.endDate > :now)")
    boolean existsActiveBanByUserId(
            @Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Ban b WHERE (b.endDate IS NULL OR b.endDate > :now)")
    List<Ban> findAllActive(@Param("now") LocalDateTime now);

}
