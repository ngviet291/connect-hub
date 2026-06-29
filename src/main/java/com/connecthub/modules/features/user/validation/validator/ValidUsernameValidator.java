package com.connecthub.modules.features.user.validation.validator;

import com.connecthub.modules.features.user.validation.annotation.ValidUsername;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[A-Za-z0-9._]*$");

    private int min;
    private int max;

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return buildViolation(context, "error.username.required");
        }

        if (value.length() < min || value.length() > max) {
            return buildViolation(context, "error.username.length");
        }

        if (!ALLOWED_CHARS.matcher(value).matches()) {
            return buildViolation(context, "error.username.invalid_chars");
        }

        return true;
    }

    private boolean buildViolation(ConstraintValidatorContext context, String messageKey) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageKey)
                .addConstraintViolation();
        return false;
    }
}