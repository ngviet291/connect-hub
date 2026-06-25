package com.connecthub.modules.features.chat.enums;

import lombok.Getter;

@Getter
public enum ChatResponseCode {
    GET_CONVERSATIONS_SUCCESS(200, "Conversations retrieved successfully"),
    GET_CONVERSATION_DETAIL_SUCCESS(200, "Conversation detail retrieved successfully"),
    MARK_AS_READ_SUCCESS(200, "Conversation marked as read successfully"),
    ACCEPT_CONVERSATION_REQUEST_SUCCESS(200, "Conversation request accepted successfully"),
    CREATE_CONVERSATION_SUCCESS(201, "Conversation created successfully"),
    UPDATE_CONVERSATION_SUCCESS(200, "Conversation updated successfully"),
    GET_MESSAGES_SUCCESS(200, "Messages retrieved successfully"),
    SEND_MESSAGE_SUCCESS(201, "Message sent successfully"),
    ADD_MEMBER_SUCCESS(200, "Member added successfully"),

    ;

    private final int code;
    private final String message;

    ChatResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
