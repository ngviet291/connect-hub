package com.connecthub.modules.features.moderation.validation.validator;

import com.connecthub.modules.features.moderation.validation.annotation.RequiredDescriptionForOther;
import com.connecthub.modules.features.moderation.validation.contract.RequiresOtherDescription;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OtherDescriptionValidator
        implements ConstraintValidator<RequiredDescriptionForOther, RequiresOtherDescription> {

    @Override
    public boolean isValid(RequiresOtherDescription value, ConstraintValidatorContext context) {
        if (value == null || value.getReason() == null) {
            return true; // @NotNull riêng sẽ lo phần required của reason
        }

        boolean isOther = "OTHER".equals(value.getReason().toString());
        if (!isOther) {
            return true;
        }

        boolean valid = value.getDescription() != null
                && !value.getDescription().trim().isEmpty();

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Description is required when reason is OTHER"
            ).addPropertyNode("description").addConstraintViolation();
        }

        return valid;
    }
}