package com.connecthub.modules.features.notification.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.dto.response.NotificationUnreadResponse;
import com.connecthub.modules.features.notification.enums.NotificationResponseCode;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable UUID id) {
        notificationService.read(id);
        return ApiResponse.<Void>builder()
                .code(NotificationResponseCode.READ_NOTIFICATION.getCode())
                .message(NotificationResponseCode.READ_NOTIFICATION.getMessage())
                .build();

    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> readAll() {
        notificationService.readAll();
        return ApiResponse.<Void>builder()
                .code(NotificationResponseCode.READ_ALL_NOTIFICATION.getCode())
                .message(NotificationResponseCode.READ_ALL_NOTIFICATION.getMessage())
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadResponse> unreadCount() {
        return ApiResponse.<NotificationUnreadResponse>builder()
                .code(NotificationResponseCode.COUNT_UNREAD.getCode())
                .message(NotificationResponseCode.COUNT_UNREAD.getMessage())
                .data(notificationService.countUnread())
                .build();
    }

    @GetMapping
    public ApiResponse<CursorResponse<NotificationResponse>> getNotification(
            UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) NotificationType type
            ) {
        return ApiResponse.<CursorResponse<NotificationResponse>>builder()
                .data(notificationService.getNotification(cursor, size, type))
                .code(NotificationResponseCode.GET_NOTIFICATION.getCode())
                .message(NotificationResponseCode.GET_NOTIFICATION.getMessage())
                .build();

    }

}
