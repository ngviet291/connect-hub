package com.connecthub.common.util;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.dto.response.ErrorResponse;
import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.modules.features.user.exception.UnauthenticatedException;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class AppUtil {
    public static ErrorResponse generateErrorResponse(HttpServletRequest request, ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();
    }

    public static Pageable buildPageable(int page, int size, String sort) {
        int validatedSize = Math.min(size, 50);
        Sort sortBy = switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };

        return PageRequest.of(page, validatedSize, sortBy);
    }


    /**
     * Lấy username của người dùng đã xác thực từ SecurityContextHolder.
     * @return Username của người dùng đã xác thực.
     * @throws AppException nếu không có người dùng nào đã xác thực hoặc người dùng không có quyền truy cập.
     */
    public static String usernameFromAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException();
        }

        return authentication.getName();
    }

    public static UUID generateUUID() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public static UUID userIdFormAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException();
        }

        String principal = authentication.getName();
        if (principal == null || principal.isBlank()) {
            throw new UnauthenticatedException();
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException ex) {
            throw new UnauthenticatedException();
        }
    }
    public static <T, R> CursorResponse<R> buildCursorResponse(
            List<T> items,
            int size,
            Function<T, UUID> cursorExtractor,
            Function<T, R> mapper
    ) {
        boolean hasNext = items.size() > size;

        if (hasNext) {
            items.removeLast();
        }

        UUID nextCursor = items.isEmpty()
                ? null
                : cursorExtractor.apply(items.getLast());

        return CursorResponse.<R>builder()
                .content(items.stream().map(mapper).toList())
                .nextCursor(nextCursor == null ? null : nextCursor.toString())
                .hasNext(hasNext)
                .build();
    }
}
