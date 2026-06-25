package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.modules.features.chat.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateMemberRoleRequest {
    private MemberRole role; // ADMIN | MEMBER
}
