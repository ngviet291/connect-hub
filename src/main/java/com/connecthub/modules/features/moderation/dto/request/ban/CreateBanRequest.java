package com.connecthub.modules.features.moderation.dto.request.ban;

import com.connecthub.modules.features.moderation.enums.BanReason;
import com.connecthub.modules.features.moderation.validation.annotation.RequiredDescriptionForOther;
import com.connecthub.modules.features.moderation.validation.contract.RequiresOtherDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredDescriptionForOther
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to create a ban for a user")
public class CreateBanRequest implements RequiresOtherDescription {
    @Schema(description = "ID of the user to be banned", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
    @Schema(description = "Reason for the ban", example = "SPAM")
    private BanReason reason;
    @Schema(description = "Description of the ban reason, required if reason is OTHER", example = "User was spamming links in comments")
    private String description;
    @Schema(description = "Start date of the ban in ISO 8601 format", example = "2024-06-01T00:00:00Z")
    private LocalDateTime startDate;
    @Schema(description = "End date of the ban in ISO 8601 format. If the end date is null, the ban is permanent", example = "2024-06-01T00:00:00Z")
    private LocalDateTime endDate;
}
