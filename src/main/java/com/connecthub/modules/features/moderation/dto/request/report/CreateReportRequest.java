package com.connecthub.modules.features.moderation.dto.request.report;

import com.connecthub.common.validation.anotation.RequiredUUID;
import com.connecthub.modules.features.moderation.enums.ReasonType;
import com.connecthub.modules.features.moderation.validation.annotation.RequiredDescriptionForOther;
import com.connecthub.modules.features.moderation.validation.annotation.ValidReportTarget;
import com.connecthub.modules.features.moderation.validation.contract.RequiresOtherDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@RequiredDescriptionForOther
@ValidReportTarget
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to create a report for a user or post. The targetUserId and postId are mutually exclusive. If the targetUserId is provided, the postId must be null, and vice versa.")
public class CreateReportRequest implements RequiresOtherDescription {
    @Schema(description = "The ID of the user being reported. This field is mutually exclusive with postId.", example = "123e4567-e89b-12d3-a456-426614174000")
    @RequiredUUID(nullable = true)
    private UUID targetUserId;
    @Schema(description = "The ID of the post being reported. This field is mutually exclusive with targetUserId.", example = "123e4567-e89b-12d3-a456-426614174000")
    @RequiredUUID(nullable = true)
    private UUID postId;
    @Schema(description = "The reason for the report...", example = "OTHER")
    @NotNull(message = "error.report.reason_required")
    private ReasonType reason;

    @Schema(description = "A description of the report, required if reason is OTHER", example = "This user is spamming the chat.")
    @Size(max = 500, message = "error.report.description_length")
    private String description;
}
