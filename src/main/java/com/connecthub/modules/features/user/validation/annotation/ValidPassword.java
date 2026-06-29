package com.connecthub.modules.features.user.validation.annotation;

import com.connecthub.modules.features.user.validation.validator.ValidPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPasswordValidator.class)
public @interface ValidPassword {

    String message() default "error.password.invalid"; // fallback, thực tế validator override theo lỗi cụ thể

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 8;

    int max() default 20;
}