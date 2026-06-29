package com.connecthub.modules.features.chat.validation.annotation;

import com.connecthub.modules.features.chat.validation.validator.ValidSendMessageTargetValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSendMessageTargetValidator.class)
public @interface ValidSendMessageTarget {

    String message() default "error.message.target_required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}