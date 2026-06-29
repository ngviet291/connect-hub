package com.connecthub.common.validation.validator;

import com.connecthub.common.validation.anotation.RequiredUUID;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.UUID;

public class RequiredUUIDValidator implements ConstraintValidator<RequiredUUID, Object> {

    private boolean nullable;

    @Override
    public void initialize(RequiredUUID constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return nullable;
        }

        if (value instanceof String || value instanceof UUID) {
            return isValidUUID(value.toString());
        }

        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return nullable;
            }
            return collection.stream().allMatch(item ->
                    (item instanceof String || item instanceof UUID) && isValidUUID(item.toString())
            );
        }

        // Type không hỗ trợ — coi như fail để tránh false positive
        return false;
    }

    private boolean isValidUUID(String value) {
        if (value == null || value.isBlank()) {
            return nullable;
        }
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}