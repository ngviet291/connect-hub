package com.connecthub.modules.features.moderation.dto.response;

import com.connecthub.modules.features.moderation.enums.ReportStatus;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateStatusResponse {
    private String reportId;
    private ReportStatus status;
    private UserSummaryResponse resolveBy;
    private LocalDateTime resolvedAt;
}
