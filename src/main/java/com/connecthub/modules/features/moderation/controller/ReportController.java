package com.connecthub.modules.features.moderation.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.modules.features.moderation.dto.request.report.CreateReportRequest;
import com.connecthub.modules.features.moderation.dto.request.report.UpdateReportStatusRequest;
import com.connecthub.modules.features.moderation.dto.response.report.MyReportResponse;
import com.connecthub.modules.features.moderation.dto.response.report.ReportDetailResponse;
import com.connecthub.modules.features.moderation.dto.response.report.ReportResponse;
import com.connecthub.modules.features.moderation.dto.response.report.UpdateStatusResponse;
import com.connecthub.modules.features.moderation.enums.ReportResponseCode;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import com.connecthub.modules.features.moderation.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<ReportResponse> createReport(@Valid @RequestBody CreateReportRequest createReportRequest) {
        return ApiResponse.<ReportResponse>builder()
                .code(ReportResponseCode.CREATED.getCode())
                .message(ReportResponseCode.CREATED.getMessage())
                .data(reportService.createReport(createReportRequest))
                .build();
    }

    @GetMapping
    public ApiResponse<PagingResponse<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable
    ) {
        return ApiResponse.<PagingResponse<ReportResponse>>builder()
                .code(ReportResponseCode.GET_REPORTS.getCode())
                .message(ReportResponseCode.GET_REPORTS.getMessage())
                .data(reportService.getReports(status, pageable))
                .build();
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ReportDetailResponse> getReport(@PathVariable UUID reportId) {
        return ApiResponse.<ReportDetailResponse>builder()
                .code(ReportResponseCode.GET_REPORT.getCode())
                .message(ReportResponseCode.GET_REPORT.getMessage())
                .data(reportService.getReportById(reportId))
                .build();
    }

    @PatchMapping("/{reportId}/status")
    public ApiResponse<UpdateStatusResponse> updateReportStatus(@PathVariable UUID reportId, @RequestBody UpdateReportStatusRequest updateReportStatusRequest) {
        return ApiResponse.<UpdateStatusResponse>builder()
                .code(ReportResponseCode.UPDATE_STATUS.getCode())
                .message(ReportResponseCode.UPDATE_STATUS.getMessage())
                .data(reportService.updateReportStatus(reportId, updateReportStatusRequest))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<CursorResponse<MyReportResponse>> getCurrentUser(@RequestParam(required = false) String status,
                                                                        @RequestParam(required = false) UUID cursor,
                                                                        @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<CursorResponse<MyReportResponse>>builder()
                .code(ReportResponseCode.GET_MY_REPORTS.getCode())
                .message(ReportResponseCode.GET_MY_REPORTS.getMessage())
                .data(reportService.getCurrentUserReports(status, cursor, limit))
                .build();

    }
}
