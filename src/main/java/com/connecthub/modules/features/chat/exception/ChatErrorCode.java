package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements BaseErrorCode {

    INVALID_CHAT_REQUEST("error.chat.invalid_chat_request", HttpStatus.BAD_REQUEST),
    REPLY_TO_MESSAGE_NOT_FOUND("error.chat.reply_to_message_not_found", HttpStatus.NOT_FOUND),
    INVALID_TYPE_CONVERSATION("error.chat.invalid_type_conversation", HttpStatus.BAD_REQUEST),
    SENDER_NOT_CONVERSATION_MEMBER("error.chat.sender_not_conversation_member", HttpStatus.FORBIDDEN),
    CONVERSATION_NOT_FOUND("error.chat.conversation_not_found", HttpStatus.NOT_FOUND),
    CONVERSATION_MEMBER_NOT_FOUND("error.chat.conversation_member_not_found", HttpStatus.NOT_FOUND),
    RECIPIENT_BLOCKED("error.chat.recipient_blocked", HttpStatus.FORBIDDEN),
    BLOCKED_BY_SENDER("error.chat.blocked_by_sender", HttpStatus.FORBIDDEN),
    MESSAGE_NOT_FOUND("error.chat.message_not_found", HttpStatus.NOT_FOUND),
    CONVERSATION_ACCESS_DENIED("error.chat.conversation_access_denied", HttpStatus.FORBIDDEN),
    GROUP_MEMBER_LIMIT_EXCEEDED("error.chat.group_member_limit_exceeded", HttpStatus.BAD_REQUEST),
    RECIPIENT_NOT_PROVIDED("error.chat.recipient_not_provided", HttpStatus.BAD_REQUEST),
    INVALID_MEMBER_STATUS("error.chat.invalid_member_status", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_ADMIN("error.chat.cannot_remove_admin", HttpStatus.BAD_REQUEST),
    CONVERSATION_PERMISSION_DENIED("error.chat.conversation_permission_denied", HttpStatus.FORBIDDEN),
    ADMIN_CANNOT_LEAVE_GROUP("error.chat.admin_cannot_leave_group", HttpStatus.BAD_REQUEST),
    MESSAGE_CONTENT_OR_MEDIA_REQUIRED("error.chat.message_content_or_media_required", HttpStatus.BAD_REQUEST),
    MEDIA_FILE_REQUIRED("error.message.media_required", HttpStatus.BAD_REQUEST),
    MEDIA_LIMIT_EXCEEDED("error.message.media_limit", HttpStatus.BAD_REQUEST),
    ;
    private final String message;
    private final HttpStatus statusCode;

    @Override
    public String toString() {
        return name();
    }

    ChatErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}