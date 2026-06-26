package com.connecthub.modules.features.moderation.validation.annotation;

import com.connecthub.modules.features.moderation.validation.validator.OtherDescriptionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OtherDescriptionValidator.class)
public @interface RequiredDescriptionForOther {
    String message() default "Description is required when reason is OTHER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}