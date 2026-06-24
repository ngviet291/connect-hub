package com.connecthub.modules.features.chat.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ChatResponseCode;
import com.connecthub.modules.features.chat.service.MessageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{messageId}")
    public void deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<CursorResponse<MessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer limit) {

        return ApiResponse.<CursorResponse<MessageResponse>>builder()
                .code(ChatResponseCode.GET_MESSAGES_SUCCESS.getCode())
                .message(ChatResponseCode.GET_MESSAGES_SUCCESS.getMessage())
                .data(messageService.getMessages(conversationId, cursor, limit))
                .build();


    }
}
