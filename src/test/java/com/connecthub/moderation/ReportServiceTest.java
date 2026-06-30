package com.connecthub.moderation;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.moderation.dto.request.report.CreateReportRequest;
import com.connecthub.modules.features.moderation.dto.request.report.UpdateReportStatusRequest;
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
import com.connecthub.modules.features.moderation.service.ReportService;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private MockedStatic<AppUtil> appUtilMock;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID REPORT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appUtilMock = mockStatic(AppUtil.class);
    }

    @AfterEach
    void tearDown() {
        appUtilMock.close();
    }

    @Nested
    @DisplayName("createReport")
    class CreateReportTest {

        @Test
        @DisplayName("Tạo report cho post thành công khi chưa có report PENDING trùng")
        void shouldCreateReportForPost_whenNoDuplicatePending() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getPostId()).thenReturn(POST_ID);
            when(request.getTargetUserId()).thenReturn(null);

            Report mappedReport = new Report();
            Post post = Post.builder().id(POST_ID).build();
            User currentUser = User.builder().id(CURRENT_USER_ID).build();
            Report savedReport = Report.builder().id(UUID.randomUUID()).post(post).reporter(currentUser).build();
            ReportResponse expectedResponse = mock(ReportResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());

            when(reportMapper.toReport(request)).thenReturn(mappedReport);
            when(reportRepository.existsByReporterIdAndPostIdAndStatus(
                    CURRENT_USER_ID, POST_ID, ReportStatus.PENDING)).thenReturn(false);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(currentUser);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(reportRepository.save(mappedReport)).thenReturn(savedReport);
            when(reportMapper.toReportResponse(savedReport)).thenReturn(expectedResponse);

            ReportResponse result = reportService.createReport(request);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(mappedReport.getReporter()).isEqualTo(currentUser);
            assertThat(mappedReport.getPost()).isEqualTo(post);
            verify(reportRepository, never()).existsByReporterIdAndTargetUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("Tạo report cho user thành công khi chưa có report PENDING trùng")
        void shouldCreateReportForUser_whenNoDuplicatePending() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getPostId()).thenReturn(null);
            when(request.getTargetUserId()).thenReturn(TARGET_USER_ID);

            Report mappedReport = new Report();
            User targetUser = User.builder().id(TARGET_USER_ID).build();
            User currentUser = User.builder().id(CURRENT_USER_ID).build();
            Report savedReport = Report.builder().id(UUID.randomUUID()).targetUser(targetUser).reporter(currentUser).build();
            ReportResponse expectedResponse = mock(ReportResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());

            when(reportMapper.toReport(request)).thenReturn(mappedReport);
            when(reportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    CURRENT_USER_ID, TARGET_USER_ID, ReportStatus.PENDING)).thenReturn(false);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(currentUser);
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
            when(reportRepository.save(mappedReport)).thenReturn(savedReport);
            when(reportMapper.toReportResponse(savedReport)).thenReturn(expectedResponse);

            ReportResponse result = reportService.createReport(request);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(mappedReport.getTargetUser()).isEqualTo(targetUser);
            verify(reportRepository, never()).existsByReporterIdAndPostIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("Ném exception khi đã có report PENDING trùng cho post")
        void shouldThrowException_whenDuplicatePendingReportForPost() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getPostId()).thenReturn(POST_ID);
            when(request.getTargetUserId()).thenReturn(null);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(reportMapper.toReport(request)).thenReturn(new Report());
            when(reportRepository.existsByReporterIdAndPostIdAndStatus(
                    CURRENT_USER_ID, POST_ID, ReportStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(DuplicatePendingReportException.class);

            verify(reportRepository, never()).save(any());
            verify(postRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Ném exception khi đã có report PENDING trùng cho target user")
        void shouldThrowException_whenDuplicatePendingReportForTargetUser() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getTargetUserId()).thenReturn(TARGET_USER_ID);
            // KHÔNG cần stub getPostId() — nhánh targetUserId != null không bao giờ gọi tới nó

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(reportMapper.toReport(request)).thenReturn(new Report());
            when(reportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    CURRENT_USER_ID, TARGET_USER_ID, ReportStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(DuplicatePendingReportException.class);

            verify(reportRepository, never()).save(any());
        }
        @Test
        @DisplayName("Ném exception khi post không tồn tại")
        void shouldThrowException_whenPostNotFound() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getPostId()).thenReturn(POST_ID);
            when(request.getTargetUserId()).thenReturn(null);

            User currentUser = User.builder().id(CURRENT_USER_ID).build();

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
            when(reportMapper.toReport(request)).thenReturn(new Report());
            when(reportRepository.existsByReporterIdAndPostIdAndStatus(
                    CURRENT_USER_ID, POST_ID, ReportStatus.PENDING)).thenReturn(false);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(currentUser);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(PostNotFoundException.class);

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném exception khi target user không tồn tại")
        void shouldThrowException_whenTargetUserNotFound() {
            CreateReportRequest request = mock(CreateReportRequest.class);
            when(request.getPostId()).thenReturn(null);
            when(request.getTargetUserId()).thenReturn(TARGET_USER_ID);

            User currentUser = User.builder().id(CURRENT_USER_ID).build();

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
            when(reportMapper.toReport(request)).thenReturn(new Report());
            when(reportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    CURRENT_USER_ID, TARGET_USER_ID, ReportStatus.PENDING)).thenReturn(false);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(currentUser);
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getReports")
    class GetReportsTest {

        @Test
        @DisplayName("Trả về danh sách report theo status filter")
        void shouldReturnReports_filteredByStatus() {
            Pageable pageable = Pageable.ofSize(10);
            Page<Report> page = new PageImpl<>(List.of(new Report()));
            PagingResponse<ReportResponse> expectedResponse = mock(PagingResponse.class);

            when(reportRepository.findByStatus(ReportStatus.PENDING, pageable)).thenReturn(page);
            appUtilMock.when(() -> AppUtil.buildPagingResponse(eq(page), any()))
                    .thenReturn(expectedResponse);

            PagingResponse<ReportResponse> result = reportService.getReports(ReportStatus.PENDING, pageable);

            assertThat(result).isEqualTo(expectedResponse);
            verify(reportRepository).findByStatus(ReportStatus.PENDING, pageable);
        }

        @Test
        @DisplayName("Trả về tất cả report khi status null")
        void shouldReturnAllReports_whenStatusIsNull() {
            Pageable pageable = Pageable.ofSize(10);
            Page<Report> page = new PageImpl<>(List.of(new Report()));
            PagingResponse<ReportResponse> expectedResponse = mock(PagingResponse.class);

            when(reportRepository.findByStatus(null, pageable)).thenReturn(page);
            appUtilMock.when(() -> AppUtil.buildPagingResponse(eq(page), any()))
                    .thenReturn(expectedResponse);

            PagingResponse<ReportResponse> result = reportService.getReports(null, pageable);

            assertThat(result).isEqualTo(expectedResponse);
            verify(reportRepository).findByStatus(null, pageable);
        }
    }

    @Nested
    @DisplayName("getReportById")
    class GetReportByIdTest {

        @Test
        @DisplayName("Trả về report detail khi tồn tại")
        void shouldReturnReportDetail_whenExists() {
            Report report = Report.builder().id(REPORT_ID).build();
            ReportDetailResponse expectedResponse = mock(ReportDetailResponse.class);

            when(reportRepository.findDetailById(REPORT_ID)).thenReturn(Optional.of(report));
            when(reportMapper.toReportDetailResponse(report)).thenReturn(expectedResponse);

            ReportDetailResponse result = reportService.getReportById(REPORT_ID);

            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Ném exception khi report không tồn tại")
        void shouldThrowException_whenReportNotFound() {
            when(reportRepository.findDetailById(REPORT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getReportById(REPORT_ID))
                    .isInstanceOf(ReportNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateReportStatus")
    class UpdateReportStatusTest {

        @Test
        @DisplayName("Cập nhật trạng thái thành công với transition hợp lệ: PENDING -> RESOLVED")
        void shouldUpdateStatus_whenTransitionIsValid_pendingToResolved() {
            UpdateReportStatusRequest request = mock(UpdateReportStatusRequest.class);
            when(request.getStatus()).thenReturn(ReportStatus.RESOLVED);

            User resolver = User.builder().id(CURRENT_USER_ID).build();
            Report report = Report.builder().id(REPORT_ID).status(ReportStatus.PENDING).build();
            UpdateStatusResponse expectedResponse = mock(UpdateStatusResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(resolver);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toUpdateReportResponse(report)).thenReturn(expectedResponse);

            UpdateStatusResponse result = reportService.updateReportStatus(REPORT_ID, request);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(report.getResolvedBy()).isEqualTo(resolver);
            verify(reportMapper).updateReport(request, report);
        }

        @Test
        @DisplayName("Cập nhật trạng thái thành công với transition hợp lệ: REVIEWING -> REJECTED")
        void shouldUpdateStatus_whenTransitionIsValid_reviewingToRejected() {
            UpdateReportStatusRequest request = mock(UpdateReportStatusRequest.class);
            when(request.getStatus()).thenReturn(ReportStatus.REJECTED);

            User resolver = User.builder().id(CURRENT_USER_ID).build();
            Report report = Report.builder().id(REPORT_ID).status(ReportStatus.REVIEWING).build();
            UpdateStatusResponse expectedResponse = mock(UpdateStatusResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(resolver);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
            when(reportRepository.save(report)).thenReturn(report);
            when(reportMapper.toUpdateReportResponse(report)).thenReturn(expectedResponse);

            UpdateStatusResponse result = reportService.updateReportStatus(REPORT_ID, request);

            assertThat(result).isEqualTo(expectedResponse);
        }

        @ParameterizedTest(name = "RESOLVED -> {0} phải bị từ chối")
        @EnumSource(ReportStatus.class)
        @DisplayName("Ném exception khi transition từ RESOLVED (trạng thái cuối, không thể đổi tiếp)")
        void shouldThrowException_whenTransitionFromResolved(ReportStatus newStatus) {
            UpdateReportStatusRequest request = mock(UpdateReportStatusRequest.class);
            when(request.getStatus()).thenReturn(newStatus);

            User resolver = User.builder().id(CURRENT_USER_ID).build();
            Report report = Report.builder().id(REPORT_ID).status(ReportStatus.RESOLVED).build();

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(resolver);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> reportService.updateReportStatus(REPORT_ID, request))
                    .isInstanceOf(InvalidReportStatusTransitionException.class);

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném exception khi transition không hợp lệ: PENDING -> PENDING (giữ nguyên)")
        void shouldThrowException_whenTransitionToSameStatus() {
            UpdateReportStatusRequest request = mock(UpdateReportStatusRequest.class);
            when(request.getStatus()).thenReturn(ReportStatus.PENDING);

            User resolver = User.builder().id(CURRENT_USER_ID).build();
            Report report = Report.builder().id(REPORT_ID).status(ReportStatus.PENDING).build();

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(resolver);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> reportService.updateReportStatus(REPORT_ID, request))
                    .isInstanceOf(InvalidReportStatusTransitionException.class);

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném exception khi report không tồn tại")
        void shouldThrowException_whenReportNotFoundForUpdate() {
            UpdateReportStatusRequest request = mock(UpdateReportStatusRequest.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(userRepository.getReferenceById(CURRENT_USER_ID))
                    .thenReturn(User.builder().id(CURRENT_USER_ID).build());
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.updateReportStatus(REPORT_ID, request))
                    .isInstanceOf(ReportNotFoundException.class);

            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentUserReports")
    class GetCurrentUserReportsTest {

        @Test
        @DisplayName("Trả về cursor response khi status hợp lệ")
        void shouldReturnCursorResponse_withValidStatus() {
            UUID cursor = UUID.randomUUID();
            int size = 10;
            List<Report> reports = List.of(new Report());
            CursorResponse<?> expectedResponse = mock(CursorResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(reportRepository.findMyReportsAndStatus(
                    eq(CURRENT_USER_ID), eq(ReportStatus.PENDING), eq(cursor), any()))
                    .thenReturn(reports);
            appUtilMock.when(() -> AppUtil.buildCursorResponse(
                            eq(reports), eq(size), any(), any()))
                    .thenReturn(expectedResponse);

            var result = reportService.getCurrentUserReports("PENDING", cursor, size);

            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Trả về cursor response khi status null/empty (không filter)")
        void shouldReturnCursorResponse_whenStatusIsNullOrEmpty() {
            UUID cursor = UUID.randomUUID();
            int size = 10;
            List<Report> reports = List.of(new Report());
            CursorResponse<?> expectedResponse = mock(CursorResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(reportRepository.findMyReportsAndStatus(
                    eq(CURRENT_USER_ID), isNull(), eq(cursor), any()))
                    .thenReturn(reports);
            appUtilMock.when(() -> AppUtil.buildCursorResponse(
                            eq(reports), eq(size), any(), any()))
                    .thenReturn(expectedResponse);

            var result = reportService.getCurrentUserReports("", cursor, size);

            assertThat(result).isEqualTo(expectedResponse);
            verify(reportRepository).findMyReportsAndStatus(CURRENT_USER_ID, null, cursor, Limit.of(size + 1));
        }

        @Test
        @DisplayName("Limit truyền vào repository phải là size + 1 (kỹ thuật xác định hasNext)")
        void shouldRequestSizePlusOneFromRepository_forCursorPaginationTechnique() {
            UUID cursor = UUID.randomUUID();
            int size = 5;

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(CURRENT_USER_ID);
            when(reportRepository.findMyReportsAndStatus(any(), any(), any(), any()))
                    .thenReturn(List.of());
            appUtilMock.when(() -> AppUtil.buildCursorResponse(any(), anyInt(), any(), any()))
                    .thenReturn(mock(CursorResponse.class));

            reportService.getCurrentUserReports(null, cursor, size);

            verify(reportRepository).findMyReportsAndStatus(
                    eq(CURRENT_USER_ID), isNull(), eq(cursor), eq(Limit.of(size + 1)));
        }
    }
}