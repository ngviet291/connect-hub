package com.connecthub.modules.features.moderation.validation.annotation;

import com.connecthub.modules.features.moderation.validation.validator.ValidBanDateRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidBanDateRangeValidator.class)
public @interface ValidBanDateRange {

    String message() default "error.ban.invalid_date_range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}