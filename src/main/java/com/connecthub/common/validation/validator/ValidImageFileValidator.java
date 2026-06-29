package com.connecthub.common.validation.validator;

import com.connecthub.common.validation.anotation.ValidImageFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class ValidImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private boolean nullable;
    private long maxSizeBytes;

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
        this.maxSizeBytes = constraintAnnotation.maxSizeBytes();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return nullable;
        }

        context.disableDefaultConstraintViolation();

        if (file.getSize() > maxSizeBytes) {
            context.buildConstraintViolationWithTemplate("error.file.size_exceeded")
                    .addConstraintViolation();
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            context.buildConstraintViolationWithTemplate("error.file.invalid_image")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}