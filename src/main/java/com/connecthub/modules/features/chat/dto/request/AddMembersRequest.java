package com.connecthub.modules.features.chat.dto.request;

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
    private List<UUID> memberIds;
}