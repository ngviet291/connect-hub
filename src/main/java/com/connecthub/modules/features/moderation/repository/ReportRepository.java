package com.connecthub.modules.features.moderation.repository;

import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
}
