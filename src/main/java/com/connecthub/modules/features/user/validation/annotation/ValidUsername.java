package com.connecthub.modules.features.user.validation.annotation;

import com.connecthub.modules.features.user.validation.validator.ValidUsernameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUsernameValidator.class)
public @interface ValidUsername {

    String message() default "error.username.invalid"; // fallback, validator override theo lỗi cụ thể

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 5;

    int max() default 20;
}