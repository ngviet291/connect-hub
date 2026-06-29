package com.connecthub.modules.features.user.validation.validator;

import com.connecthub.modules.features.user.validation.annotation.ValidPhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // E.164: + tối đa 15 số, hoặc số nội địa VN bắt đầu bằng 0
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern VN_LOCAL = Pattern.compile("^0\\d{9}$");

    private boolean nullable;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return nullable;
        }

        String trimmed = value.trim();
        boolean valid = E164.matcher(trimmed).matches() || VN_LOCAL.matcher(trimmed).matches();

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("error.phonenumber.invalid")
                    .addConstraintViolation();
        }

        return valid;
    }
}