package com.connecthub.common.advice;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Resolves message keys (e.g. "success.notification.read") on ApiResponse
 * into localized text before serialization, based on the request's
 * Accept-Language header (handled by LocaleResolver).
 * <p>
 * This means controllers can keep doing:
 * .message(NotificationResponseCode.READ_NOTIFICATION.getMessage())
 * which returns the raw key, and the actual translated string is filled
 * in here automatically — no per-controller changes needed.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ApiResponseMessageAdvice implements ResponseBodyAdvice<Object> {

    private final MessageUtil messageUtil;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof ApiResponse<?> apiResponse) {
            String message = apiResponse.getMessage();
            if (message != null && looksLikeMessageKey(message)) {
                apiResponse.setMessage(messageUtil.get(message));
            }
        }
        return body;
    }

    /**
     * Heuristic: our i18n keys always use the "success." or "error." prefix
     * with dot-separated lowercase segments (e.g. "success.notification.read").
     * Plain free-text messages (e.g. "Validation failed") won't match this
     * and are passed through untouched.
     */
    private boolean looksLikeMessageKey(String message) {
        return message.matches("^(success|error)(\\.[a-z0-9_]+)+$");
    }
}