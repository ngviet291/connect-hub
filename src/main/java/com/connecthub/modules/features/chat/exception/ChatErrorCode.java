package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements BaseErrorCode {

    INVALID_CHAT_REQUEST("Invalid chat request", HttpStatus.BAD_REQUEST),
    REPLY_TO_MESSAGE_NOT_FOUND("Reply to message {messageId} not found", HttpStatus.NOT_FOUND),
    INVALID_TYPE_CONVERSATION("Conversation is not {conversationType}", HttpStatus.BAD_REQUEST),
    SENDER_NOT_CONVERSATION_MEMBER("Sender is not a member of the conversation", HttpStatus.FORBIDDEN),
    CONVERSATION_NOT_FOUND("Conversation not found", HttpStatus.NOT_FOUND),
    CONVERSATION_MEMBER_NOT_FOUND("Conversation member not found", HttpStatus.NOT_FOUND),
    RECIPIENT_BLOCKED("You have blocked this user", HttpStatus.FORBIDDEN),
    BLOCKED_BY_SENDER("You have been blocked by this user", HttpStatus.FORBIDDEN),
    MESSAGE_NOT_FOUND("Message {messageId} not found", HttpStatus.NOT_FOUND),
    CONVERSATION_ACCESS_DENIED("You do not have access to this conversation", HttpStatus.FORBIDDEN),
    GROUP_MEMBER_LIMIT_EXCEEDED("Group member limit of {maxGroupMember} exceeded", HttpStatus.BAD_REQUEST),
    RECIPIENT_NOT_PROVIDED("Recipient not provided", HttpStatus.BAD_REQUEST),
    INVALID_MEMBER_STATUS("Invalid member status: {status}", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_ADMIN("Cannot remove admin from the group", HttpStatus.BAD_REQUEST),
    CONVERSATION_PERMISSION_DENIED("You do not have permission to access this conversation", HttpStatus.FORBIDDEN),
    ADMIN_CANNOT_LEAVE_GROUP("Admin cannot leave the group", HttpStatus.BAD_REQUEST),

    ;
    private final String message;
    private final HttpStatus statusCode;

    ChatErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
