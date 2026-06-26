package com.connecthub.modules.features.moderation.repository;

import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    @Query("""
            SELECT r FROM Report r
            LEFT JOIN FETCH r.reporter
            LEFT JOIN FETCH r.targetUser
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.resolvedBy
            WHERE (:status IS NULL OR r.status = :status)
            """)
    Page<Report> findByStatus(@Param("status") ReportStatus status, Pageable pageable);

    Page<Report> findByReporterId(UUID reporterId, Pageable pageable);

    Page<Report> findByTargetUserId(UUID targetUserId, Pageable pageable);

    Page<Report> findByPostId(UUID postId, Pageable pageable);

    boolean existsByReporterIdAndTargetUserIdAndStatus(
            UUID reporterId, UUID targetUserId, ReportStatus status);

    boolean existsByReporterIdAndPostIdAndStatus(
            UUID reporterId, UUID postId, ReportStatus status);

    @Query("""
            select r from Report r
            left join fetch r.reporter
            left join fetch r.targetUser
            left join fetch r.post p
            left join fetch p.user
            left join fetch r.resolvedBy
            where r.id = :id
            """)
    Optional<Report> findDetailById(@Param("id") UUID id);

    @Query("""
            SELECT r
            FROM Report r
                  LEFT JOIN FETCH r.targetUser
                  LEFT JOIN FETCH r.post p
                  LEFT JOIN FETCH p.user
            WHERE r.reporter.id = :reporterId
            AND (:reportStatus IS NULL OR r.status = :reportStatus)
            AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    List<Report> findMyReportsAndStatus(UUID reporterId, ReportStatus reportStatus, UUID cursor, Limit limit);
}
