package com.connecthub.modules.features.moderation.dto.request.report;

import com.connecthub.modules.features.moderation.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to update the status of a report")
public class UpdateReportStatusRequest {

    @Schema(description = "The new status of the report", example = "RESOLVED")
    @NotNull(message = "error.report.status_required")
    private ReportStatus status;

    @Schema(description = "The note explaining the resolution of the report", example = "The reported content was found to be in violation of our community guidelines.")
    @NotBlank(message = "error.report.resolution_note_required")
    @Size(max = 1000, message = "error.report.resolution_note_length")
    private String resolutionNote;
}