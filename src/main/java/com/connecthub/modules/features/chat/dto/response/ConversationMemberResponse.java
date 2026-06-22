package com.connecthub.modules.features.chat.dto.response;

import com.connecthub.modules.features.chat.enums.MemberRole;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class ConversationMemberResponse {
    private UUID userId;
    private String username;        // HIỆN TẠI, join trực tiếp từ User
    private String avatarUrl;       // HIỆN TẠI
    private MemberRole role;        // ADMIN | MEMBER (group); null/MEMBER cho private
    private MemberStatus status;    // ACCEPTED | PENDING | BLOCKED
}