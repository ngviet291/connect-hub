package com.connecthub.modules.features.post.validation.validator;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.validation.annotation.ValidPostRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPostRequestValidator implements ConstraintValidator<ValidPostRequest, PostRequest> {

    @Override
    public boolean isValid(PostRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // để @NotNull (nếu có) xử lý riêng
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // Rule 1: không cho vừa comment vừa quote
        boolean hasParent = request.getParentPostId() != null;
        boolean hasQuote = request.getQuotePostId() != null;

        if (hasParent && hasQuote) {
            context.buildConstraintViolationWithTemplate("error.post.parent_and_quote_conflict")
                    .addPropertyNode("parentPostId")
                    .addConstraintViolation();
            valid = false;
        }

        // Rule 2: phải có ít nhất content HOẶC files
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasFiles = request.getFiles() != null && !request.getFiles().isEmpty();

        if (!hasContent && !hasFiles) {
            context.buildConstraintViolationWithTemplate("error.post.content_or_files_required")
                    .addPropertyNode("content")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}