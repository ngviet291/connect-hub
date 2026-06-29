package com.connecthub.modules.features.chat.enums;

import lombok.Getter;

@Getter
public enum ChatResponseCode {
    GET_CONVERSATIONS_SUCCESS(200, "success.chat.get_conversations"),
    GET_CONVERSATION_DETAIL_SUCCESS(200, "success.chat.get_conversation_detail"),
    MARK_AS_READ_SUCCESS(200, "success.chat.mark_as_read"),
    ACCEPT_CONVERSATION_REQUEST_SUCCESS(200, "success.chat.accept_conversation_request"),
    CREATE_CONVERSATION_SUCCESS(201, "success.chat.create_conversation"),
    UPDATE_CONVERSATION_SUCCESS(200, "success.chat.update_conversation"),
    GET_MESSAGES_SUCCESS(200, "success.chat.get_messages"),
    SEND_MESSAGE_SUCCESS(201, "success.chat.send_message"),
    ADD_MEMBER_SUCCESS(200, "success.chat.add_member"),

    ;

    private final int code;
    private final String message;

    ChatResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}