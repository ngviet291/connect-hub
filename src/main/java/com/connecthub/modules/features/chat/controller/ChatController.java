package com.connecthub.modules.features.chat.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ChatResponseCode;
import com.connecthub.modules.features.chat.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest messageRequest) {
        return ApiResponse.<MessageResponse>builder()
                .message(ChatResponseCode.SEND_MESSAGE_SUCCESS.getMessage())
                .data(chatService.sendMessage(messageRequest))
                .code(ChatResponseCode.SEND_MESSAGE_SUCCESS.getCode())
                .build();
    }

    @PutMapping("/{conversationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestParam @Valid UUID lastMessageId
    ) {
        chatService.markConversationAsRead(conversationId, lastMessageId);
        return ApiResponse.<Void>builder()
                .code(ChatResponseCode.MARK_AS_READ_SUCCESS.getCode())
                .message(ChatResponseCode.MARK_AS_READ_SUCCESS.getMessage())
                .build();
    }


}
