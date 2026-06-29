package com.connecthub.modules.features.chat.validation.validator;

import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.exception.*;
import com.connecthub.modules.features.chat.validation.annotation.ValidSendMessageTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSendMessageTargetValidator implements ConstraintValidator<ValidSendMessageTarget, SendMessageRequest> {

    @Override
    public boolean isValid(SendMessageRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean hasConversation = request.getConversationId() != null;
        boolean hasRecipient = request.getRecipientId() != null;
        if (!hasConversation && !hasRecipient) {
            throw new RecipientNotProvidedException();
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        if (!hasContent && !hasMedia) {
            throw new MessageContentOrMediaRequiredException();
        }

        return true;
    }
}