package com.connecthub.modules.features.moderation.dto.request;

import com.connecthub.modules.features.moderation.enums.ReportStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateStatusRequest {
    private ReportStatus status;
    private String resolutionNote;
}
