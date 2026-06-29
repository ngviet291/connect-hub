package com.connecthub.modules.features.chat.validation.validator;

import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.validation.annotation.ValidSendMessageTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSendMessageTargetValidator implements ConstraintValidator<ValidSendMessageTarget, SendMessageRequest> {

    @Override
    public boolean isValid(SendMessageRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        boolean hasConversation = request.getConversationId() != null;
        boolean hasRecipient = request.getRecipientId() != null;
        if (!hasConversation && !hasRecipient) {
            context.buildConstraintViolationWithTemplate("error.message.target_required")
                    .addPropertyNode("conversationId")
                    .addConstraintViolation();
            valid = false;
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        if (!hasContent && !hasMedia) {
            context.buildConstraintViolationWithTemplate("error.message.content_or_media_required")
                    .addPropertyNode("content")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}