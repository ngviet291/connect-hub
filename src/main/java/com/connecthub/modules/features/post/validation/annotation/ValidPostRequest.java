package com.connecthub.modules.features.post.validation.annotation;

import com.connecthub.modules.features.post.validation.validator.ValidPostRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPostRequestValidator.class)
public @interface ValidPostRequest {

    String message() default "error.post.invalid"; // fallback, validator override theo lỗi cụ thể

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}