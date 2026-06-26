package com.connecthub.modules.features.moderation.validation.annotation;

import com.connecthub.modules.features.moderation.validation.validator.ReportTargetValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReportTargetValidator.class)
public @interface ValidReportTarget {
    String message() default "Exactly one of targetUserId or postId must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}