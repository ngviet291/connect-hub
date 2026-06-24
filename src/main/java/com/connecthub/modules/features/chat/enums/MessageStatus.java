package com.connecthub.modules.features.chat.enums;

public enum MessageStatus {
    SENT,
    DELIVERED,
    READ, // chỉ áp dụng cho PRIVATE; GROUP không dùng giá trị này
    DELETED;
}
