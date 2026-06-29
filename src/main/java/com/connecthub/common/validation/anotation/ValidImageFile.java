package com.connecthub.common.validation.anotation;

import com.connecthub.common.validation.validator.ValidImageFileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidImageFileValidator.class)
public @interface ValidImageFile {

    String message() default "error.file.invalid_image";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean nullable() default true;

    long maxSizeBytes() default 5 * 1024 * 1024; // 5MB default
}