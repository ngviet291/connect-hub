package com.connecthub.modules.features.moderation.validation.validator;

import com.connecthub.modules.features.moderation.dto.request.ban.CreateBanRequest;
import com.connecthub.modules.features.moderation.validation.annotation.ValidBanDateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class ValidBanDateRangeValidator implements ConstraintValidator<ValidBanDateRange, CreateBanRequest> {

    @Override
    public boolean isValid(CreateBanRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        // endDate null = permanent ban -> hợp lệ
        if (request.getEndDate() == null) {
            return true;
        }

        // startDate null thì để @NotNull trên field đó tự bắt riêng,
        // ở đây không so sánh được nên coi như pass để tránh đè message
        if (request.getStartDate() == null) {
            return true;
        }

       
        boolean valid = request.getEndDate().isAfter(request.getStartDate());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("error.ban.invalid_date_range")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
        }

        return valid;
    }
}