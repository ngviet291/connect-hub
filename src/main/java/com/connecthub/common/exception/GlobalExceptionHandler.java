package com.connecthub.common.exception;

import com.connecthub.common.dto.response.AccountLockedErrorResponse;
import com.connecthub.common.dto.response.ErrorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.common.util.MessageUtil;
import com.connecthub.modules.features.user.exception.AccountLockedException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageUtil messageUtil;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode().value())
                .message(messageUtil.get(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()))
                .timestamp(LocalDateTime.now())
                .error(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("Uncaught exception at [{}]", request.getRequestURI(), ex);

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        // Field-level errors (đã i18n qua MessageSource gắn vào LocalValidatorFactoryBean, nếu đã config)
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        ));

        // Class-level (object) errors
        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(error ->
                        errors.put(
                                error.getObjectName(),
                                error.getDefaultMessage()
                        ));

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                messageUtil.get(ErrorCode.VALIDATION_FAILED.getMessage()),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .status(ex.getErrorCode().getStatusCode().value())
                .message(messageUtil.get(ex.getErrorCode().getMessage()))
                .error(HttpStatus.valueOf(ex.getErrorCode().getStatusCode().value()).getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode()).body(response);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode())
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(ErrorCode.FORBIDDEN.getStatusCode().value())
                        .error(HttpStatus.valueOf(ErrorCode.FORBIDDEN.getStatusCode().value()).getReasonPhrase())
                        .message(messageUtil.get(ErrorCode.FORBIDDEN.getMessage()))
                        .path(request.getRequestURI())
                        .build());
    }

    private String mapAttribute(String message, String attributeKey, String attributeValue) {
        if (message.contains("{" + attributeKey + "}")) {
            return message.replace("{" + attributeKey + "}", attributeValue);
        }
        return message;
    }

    private String mapAttributes(String message, Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            message = mapAttribute(message, entry.getKey(), entry.getValue().toString());
        }
        return message;
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;


        Class<?> errorTypeClass = e.getRequiredType();

        Set<Class<?>> NUMBER_TYPES = Set.of(
                Byte.class, Short.class, Integer.class, Long.class,
                Float.class, Double.class,
                byte.class, short.class, int.class, long.class,
                float.class, double.class
        );
        if (errorTypeClass == null) {
            errorTypeClass = e.getParameter().getParameterType();
        }

        if (errorTypeClass == LocalDate.class) {
            errorCode = ErrorCode.INVALID_DATE_FORMAT;
        } else if (NUMBER_TYPES.contains(errorTypeClass)) {
            errorCode = ErrorCode.INVALID_NUMBER_FORMAT;
        }

        String message = messageUtil.get(errorCode.getMessage());
        if (e.getValue() != null) {
            Map<String, Object> params = Map.of(
                    "value", e.getValue(),
                    "parameter", e.getName(),
                    "type", errorTypeClass.getSimpleName()
            );
            message = mapAttributes(message, params);
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }

    @ExceptionHandler(ParameterizedException.class)
    public ResponseEntity<ErrorResponse> handleParameterizedException(ParameterizedException e, HttpServletRequest request) {

        String message = e.getErrorCode().getMessage();
        message = messageUtil.get(message);
        Map<String, Object> parameters = e.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            message = mapAttributes(message, parameters);
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(e.getErrorCode().getStatusCode().value())
                .error(HttpStatus.valueOf(e.getErrorCode().getStatusCode().value()).getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(e.getErrorCode().getStatusCode()).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED;

        ErrorResponse errorResponse = AppUtil.generateErrorResponse(request, errorCode);

        return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);

    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AccountLockedErrorResponse> handleAccountLocked(
            AccountLockedException ex, HttpServletRequest request) {

        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = Math.max(
                Duration.between(now, ex.getLockedUntil()).getSeconds(), 0
        );

        String message = mapAttribute(
                messageUtil.get(ex.getErrorCode().getMessage()),
                "remainingSeconds",
                String.valueOf(remainingSeconds)
        );

        AccountLockedErrorResponse error = AccountLockedErrorResponse.builder()
                .timestamp(now)
                .status(HttpStatus.FORBIDDEN.value())
                .error(ex.getErrorCode().toString())
                .message(message)
                .path(request.getRequestURI())
                .lockedUntil(ex.getLockedUntil())
                .remainingSeconds(remainingSeconds)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        Throwable cause = ex.getCause();
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        String message;

        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String fieldName = ife.getPath().isEmpty()
                    ? "field"
                    : ife.getPath().get(ife.getPath().size() - 1).getFieldName();

            Object[] acceptedValues = ife.getTargetType().getEnumConstants();
            String accepted = Arrays.stream(acceptedValues)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            message = mapAttributes(
                    messageUtil.get("error.invalid_enum_value"),
                    Map.of("field", fieldName, "value", ife.getValue(), "accepted", accepted)
            );
        } else {
            message = messageUtil.get(errorCode.getMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }
}