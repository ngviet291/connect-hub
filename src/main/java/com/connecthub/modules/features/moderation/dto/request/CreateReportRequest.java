package com.connecthub.modules.features.moderation.dto.request;

import com.connecthub.modules.features.moderation.enums.ReasonType;
import com.connecthub.modules.features.moderation.validation.annotation.ValidReportTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@ValidReportTarget
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to create a report for a user or post. The targetUserId and postId are mutually exclusive. If the targetUserId is provided, the postId must be null, and vice versa.")
public class CreateReportRequest {
    @Schema(description = "The ID of the user being reported. This field is mutually exclusive with postId.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID targetUserId;
    @Schema(description = "The ID of the post being reported. This field is mutually exclusive with targetUserId.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID postId;
    @Schema(description = "The reason for the report. This field is required and must be one of the predefined reason types.", example = "OTHER")
    private ReasonType reason;
    @Schema(description = "A description of the report.", example = "This user is spamming the chat.")
    private String description;
}
