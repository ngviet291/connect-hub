package com.connecthub.modules.features.moderation.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.moderation.dto.request.report.CreateReportRequest;
import com.connecthub.modules.features.moderation.dto.request.report.UpdateReportStatusRequest;
import com.connecthub.modules.features.moderation.dto.response.report.MyReportResponse;
import com.connecthub.modules.features.moderation.dto.response.report.ReportDetailResponse;
import com.connecthub.modules.features.moderation.dto.response.report.ReportResponse;
import com.connecthub.modules.features.moderation.dto.response.report.UpdateStatusResponse;
import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import com.connecthub.modules.features.moderation.exception.report.DuplicatePendingReportException;
import com.connecthub.modules.features.moderation.exception.report.InvalidReportStatusTransitionException;
import com.connecthub.modules.features.moderation.exception.report.ReportNotFoundException;
import com.connecthub.modules.features.moderation.mapper.ReportMapper;
import com.connecthub.modules.features.moderation.repository.ReportRepository;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {


    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        Report reportEntity = reportMapper.toReport(request);
        UUID currentUserId = AppUtil.userIdFromAuthentication();
        if (request.getTargetUserId() != null) {
            if (reportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    currentUserId, request.getTargetUserId(), ReportStatus.PENDING)) {
                throw new DuplicatePendingReportException();
            }
        } else {
            if (reportRepository.existsByReporterIdAndPostIdAndStatus(
                    currentUserId, request.getPostId(), ReportStatus.PENDING)) {
                throw new DuplicatePendingReportException();
            }
        }

        User currentUser = userRepository.getReferenceById(AppUtil.userIdFromAuthentication());
        reportEntity.setId(AppUtil.generateUUID());

        reportEntity.setReporter(currentUser);

        if (request.getPostId() != null) {
            Post post = postRepository.findById(request.getPostId()).orElseThrow(PostNotFoundException::new);
            reportEntity.setPost(post);
        } else {
            User user = userRepository.findById(request.getTargetUserId()).orElseThrow(UserNotFoundException::new);
            reportEntity.setTargetUser(user);
        }
        return reportMapper.toReportResponse(reportRepository.save(reportEntity));
    }


    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public PagingResponse<ReportResponse> getReports(@RequestParam(required = false) ReportStatus status, Pageable pageable) {
        Page<Report> reportsPage = reportRepository.findByStatus(status, pageable);
        return AppUtil.buildPagingResponse(reportsPage, reportMapper::toReportResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public ReportDetailResponse getReportById(UUID reportId) {
        Report report = reportRepository.findDetailById(reportId).orElseThrow(ReportNotFoundException::new);

        return reportMapper.toReportDetailResponse(report);
    }

    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public UpdateStatusResponse updateReportStatus(UUID reportId, UpdateReportStatusRequest updateReportStatusRequest) {

        User userResolve = userRepository.getReferenceById(AppUtil.userIdFromAuthentication());

        Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);
        validateStatus(report.getStatus(), updateReportStatusRequest.getStatus());
        reportMapper.updateReport(updateReportStatusRequest, report);
        report.setResolvedBy(userResolve);
        return reportMapper.toUpdateReportResponse(reportRepository.save(report));
    }

    // helper method to validate status transition
    private void validateStatus(ReportStatus currentStatus, ReportStatus newStatus) {
        Set<ReportStatus> validTransitions = switch (currentStatus) {
            case PENDING -> Set.of(ReportStatus.RESOLVED, ReportStatus.REJECTED, ReportStatus.REVIEWING);
            case REVIEWING -> Set.of(ReportStatus.RESOLVED, ReportStatus.REJECTED);
            case RESOLVED, REJECTED -> Set.of();
        };
        if (!validTransitions.contains(newStatus)) {
            throw new InvalidReportStatusTransitionException(currentStatus.name(), newStatus.name());
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public CursorResponse<MyReportResponse> getCurrentUserReports(String status, UUID cursor, int size) {
        ReportStatus reportStatus = null;
        if (status != null && !status.isEmpty()) {
            reportStatus = ReportStatus.fromString(status);
        }

        Limit limit = Limit.of(size + 1);

        List<Report> reports = new ArrayList<>(
                reportRepository.findMyReportsAndStatus(AppUtil.userIdFromAuthentication(), reportStatus, cursor, limit)
        );

        return AppUtil.buildCursorResponse(
                reports,
                size,
                Report::getId,
                reportMapper::toMyReportResponse
        );
    }
}
