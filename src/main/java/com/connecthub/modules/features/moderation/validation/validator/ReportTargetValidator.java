package com.connecthub.modules.features.moderation.validation.validator;

import com.connecthub.modules.features.moderation.dto.request.CreateReportRequest;
import com.connecthub.modules.features.moderation.validation.annotation.ValidReportTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReportTargetValidator
        implements ConstraintValidator<ValidReportTarget, CreateReportRequest> {


    @Override
    public boolean isValid(CreateReportRequest createReportRequest, ConstraintValidatorContext constraintValidatorContext) {
        boolean hasUser = createReportRequest.getTargetUserId() != null;
        boolean hasPost = createReportRequest.getPostId() != null;

        return hasUser ^ hasPost;
    }
}