package com.connecthub.modules.features.chat.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.chat.dto.request.AcceptConversationRequest;
import com.connecthub.modules.features.chat.dto.response.ConversationDetailResponse;
import com.connecthub.modules.features.chat.dto.response.ConversationResponse;
import com.connecthub.modules.features.chat.dto.response.ConversationSummaryResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.enums.ChatResponseCode;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.service.ChatService;
import com.connecthub.modules.features.chat.service.ConversationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    private final ChatService chatService;

    @PatchMapping("/accept")
    public void acceptConversationRequest(AcceptConversationRequest request) {
        conversationService.acceptConversationMember(request);
        // Implementation for accepting a conversation request
    }

    @GetMapping
    public ApiResponse<CursorResponse<ConversationSummaryResponse>> getConversations(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) MemberStatus status
    ) {
        return ApiResponse.<CursorResponse<ConversationSummaryResponse>>builder()
                .code(ChatResponseCode.GET_CONVERSATIONS_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CONVERSATIONS_SUCCESS.getMessage())
                .data(conversationService.getConversations(cursor, size, status))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationDetailResponse> getConversationDetail(@PathVariable UUID id) {
        return ApiResponse.<ConversationDetailResponse>builder()
                .code(ChatResponseCode.GET_CONVERSATION_DETAIL_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CONVERSATION_DETAIL_SUCCESS.getMessage())
//                .data(conversationService.getConversationDetail(id))
                .build();
    }

    @PutMapping("/{conversationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestParam UUID lastMessageId
    ) {
        chatService.markConversationAsRead(conversationId, lastMessageId);
        return ApiResponse.<Void>builder()
                .code(ChatResponseCode.MARK_AS_READ_SUCCESS.getCode())
                .message(ChatResponseCode.MARK_AS_READ_SUCCESS.getMessage())
                .build();
    }
}
