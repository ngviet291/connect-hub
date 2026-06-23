package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements BaseErrorCode {

    INVALID_CHAT_REQUEST("Message must have content or media", HttpStatus.BAD_REQUEST),
    REPLY_TO_MESSAGE_NOT_FOUND("Reply to message {messageId} not found", HttpStatus.NOT_FOUND),
    INVALID_TYPE_CONVERSATION("Conversation is not {conversationType}", HttpStatus.BAD_REQUEST),
    SENDER_NOT_CONVERSATION_MEMBER("Sender is not a member of the conversation", HttpStatus.FORBIDDEN),
    CONVERSATION_NOT_FOUND("Conversation not found", HttpStatus.NOT_FOUND),
    CONVERSATION_MEMBER_NOT_FOUND("Conversation member not found", HttpStatus.NOT_FOUND),
    RECIPIENT_BLOCKED("You have blocked this user", HttpStatus.FORBIDDEN),
    BLOCKED_BY_SENDER("You have been blocked by this user", HttpStatus.FORBIDDEN),
    MESSAGE_NOT_FOUND("Message {messageId} not found", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus statusCode;

    ChatErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
