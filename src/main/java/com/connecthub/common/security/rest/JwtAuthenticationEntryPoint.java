package com.connecthub.common.security.rest;

import com.connecthub.common.dto.response.ErrorResponse;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.user.exception.AccountLockedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {


    // handle unauthorized access attempts by sending a JSON response with error details
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        ErrorCode errorCode = getErrorCode(authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorCode.getStatusCode().value());

        ErrorResponse errorResponse = AppUtil.generateErrorResponse(request, errorCode);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.flushBuffer();

    }

    // Phương thức này sẽ kiểm tra nguyên nhân gốc của AuthenticationException để xác định mã lỗi phù hợp
    private static ErrorCode getErrorCode(AuthenticationException authException) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        // Kiểm tra xem lỗi nguyên nhân gốc (Root Cause) có phải là AccountLockedException không
        if (authException.getCause() instanceof AccountLockedException) {
            errorCode = ErrorCode.ACCOUNT_LOCKED; // Gán mã lỗi tài khoản bị khóa của bạn
        }
        // Dự phòng trường hợp lỗi bị bọc sâu thêm 1 tầng nữa
        else if (authException.getCause() != null && authException.getCause().getCause() instanceof AccountLockedException) {
            errorCode = ErrorCode.ACCOUNT_LOCKED;
        }
        return errorCode;
    }
}
