package com.connecthub.modules.features.user.validation.validator;

import com.connecthub.modules.features.user.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[@$!%*?&].*");
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[A-Za-z\\d@$!%*?&]*$");

    private int min;
    private int max;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return buildViolation(context, "error.password.required");
        }

        if (value.length() < min || value.length() > max) {
            return buildViolation(context, "error.password.length");
        }

        if (!ALLOWED_CHARS.matcher(value).matches()) {
            return buildViolation(context, "error.password.invalid_chars");
        }

        if (!LOWERCASE.matcher(value).matches()) {
            return buildViolation(context, "error.password.missing_lowercase");
        }

        if (!UPPERCASE.matcher(value).matches()) {
            return buildViolation(context, "error.password.missing_uppercase");
        }

        if (!DIGIT.matcher(value).matches()) {
            return buildViolation(context, "error.password.missing_digit");
        }

        if (!SPECIAL.matcher(value).matches()) {
            return buildViolation(context, "error.password.missing_special");
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