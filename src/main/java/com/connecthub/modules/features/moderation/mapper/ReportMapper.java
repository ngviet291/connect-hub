package com.connecthub.modules.features.moderation.mapper;

import com.connecthub.modules.features.moderation.dto.request.CreateReportRequest;
import com.connecthub.modules.features.moderation.dto.request.UpdateStatusRequest;
import com.connecthub.modules.features.moderation.dto.response.*;
import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ReportMapper {
    Report toReport(CreateReportRequest report);

    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "targetUserId", source = "targetUser.id")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "reportId", source = "id")
    ReportResponse toReportResponse(Report report);

    ReportDetailResponse toReportDetailResponse(Report report);

    @Mapping(target = "author", source = "user")
    PostSummaryResponse toPostSummaryResponse(Post post);

    @Mapping(target = "status", source = "updateStatusRequest.status")
    @Mapping(target = "resolvedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "resolutionNote", source = "updateStatusRequest.resolutionNote")
    void updateReport(UpdateStatusRequest updateStatusRequest, @MappingTarget Report report);

    @Mapping(target = "reportId", source = "id")
    UpdateStatusResponse toUpdateReportResponse(Report save);

    @Mapping(target = "reportId", source = "id")
    MyReportResponse toMyReportResponse(Report report);
}
