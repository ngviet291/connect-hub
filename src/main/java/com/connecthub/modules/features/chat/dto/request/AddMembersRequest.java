package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.common.validation.anotation.RequiredUUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddMembersRequest {
    // validate that the memberIds are not empty and that they are valid UUIDs
    @NotEmpty(message = "error.member.member_ids_required")
    @RequiredUUID
    private List<UUID> memberIds;
}