package com.connecthub.modules.features.moderation.dto.request;

import com.connecthub.modules.features.moderation.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to update the status of a report")
public class UpdateStatusRequest {
    @Schema(description = "The new status of the report", example = "RESOLVED")
    private ReportStatus status;
    @Schema(description = "The note explaining the resolution of the report", example = "The reported content was found to be in violation of our community guidelines.")
    private String resolutionNote;
}
