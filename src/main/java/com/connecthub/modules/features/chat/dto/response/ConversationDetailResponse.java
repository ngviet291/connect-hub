package com.connecthub.modules.features.chat.dto.response;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class ConversationDetailResponse {
    private UUID conversationId;
    private ConversationType type;
    private String displayName;
    private String displayAvatarUrl;
    private MemberStatus myStatus;

    // Đầy đủ thành viên — cần cho group (admin badge, danh sách member),
    // với PRIVATE vẫn trả về (sẽ có đúng 2 entry) để FE dùng chung 1 shape.
    private CursorResponse<ConversationMemberResponse> members;

    private LocalDateTime createdAt;
}

