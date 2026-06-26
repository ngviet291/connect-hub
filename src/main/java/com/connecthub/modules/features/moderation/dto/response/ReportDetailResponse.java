package com.connecthub.modules.features.moderation.dto.response;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDetailResponse {
    private String id;
    private UserSummaryResponse reporter;
    private UserSummaryResponse targetUser;
    private PostSummaryResponse post;
    private String reason;
    private String description;
    private String status;
    private UserSummaryResponse resolvedBy;
    private String resolutionNote;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
