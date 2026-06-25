package com.connecthub.modules.features.chat.enums;

import com.connecthub.modules.features.chat.exception.InvalidMemberStatusException;

public enum MemberStatus {
    PENDING,  // Tin nhắn chờ chấp nhận
    ACCEPTED, // Đã chấp nhận – nhắn tin 2 chiều
    BLOCKED,   // Người nhận từ chối / bị chặn
    REMOVED,   // Người nhận bị kick khỏi cuộc trò chuyện chỉ tồn tại trong group chat
    LEFT,    // Người nhận rời khỏi cuộc trò chuyện chỉ tồn tại trong group chat

    ;
    public static MemberStatus fromString(String status) {
        for (MemberStatus memberStatus : MemberStatus.values()) {
            if (memberStatus.name().equalsIgnoreCase(status)) {
                return memberStatus;
            }
        }
        throw new InvalidMemberStatusException(status);
    }

}
