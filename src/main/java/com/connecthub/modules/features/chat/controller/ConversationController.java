package com.connecthub.modules.features.chat.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.chat.dto.request.*;
import com.connecthub.modules.features.chat.dto.response.ConversationDetailResponse;
import com.connecthub.modules.features.chat.dto.response.ConversationSummaryResponse;
import com.connecthub.modules.features.chat.enums.ChatResponseCode;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.service.ConversationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @PatchMapping("/accept")
    public ApiResponse<Void> acceptConversationRequest(@RequestBody AcceptConversationRequest request) {
        conversationService.acceptConversationMember(request);
        return ApiResponse.<Void>builder()
                .code(ChatResponseCode.ACCEPT_CONVERSATION_REQUEST_SUCCESS.getCode())
                .message(ChatResponseCode.ACCEPT_CONVERSATION_REQUEST_SUCCESS.getMessage())
                .build();
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
    public ApiResponse<ConversationDetailResponse> getConversationDetail(@PathVariable UUID id, @RequestParam(required = false) UUID cursor, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<ConversationDetailResponse>builder()
                .code(ChatResponseCode.GET_CONVERSATION_DETAIL_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CONVERSATION_DETAIL_SUCCESS.getMessage())
                .data(conversationService.getConversationDetail(id, cursor, size))
                .build();
    }


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/group")
    public ApiResponse<ConversationSummaryResponse> createPublicConversation(@RequestBody CreateGroupConversationRequest request) {
        return ApiResponse.<ConversationSummaryResponse>builder()
                .code(ChatResponseCode.CREATE_CONVERSATION_SUCCESS.getCode())
                .message(ChatResponseCode.CREATE_CONVERSATION_SUCCESS.getMessage())
                .data(conversationService.createGroupConversation(request))
                .build();
    }

    @PatchMapping("/{conversationId}/leave")
    public void leaveConversation(@PathVariable UUID conversationId) {
        conversationService.leaveConversation(conversationId);
    }

    @PatchMapping("/{conversationId}/members/{memberId}/remove")
    public void removeMember(@PathVariable UUID conversationId, @PathVariable UUID memberId) {
        conversationService.removeMember(conversationId, memberId);
    }

    @PatchMapping("/{conversationId}")
    public ApiResponse<ConversationSummaryResponse> updateConversation(@PathVariable UUID conversationId, @ModelAttribute UpdateConversationRequest request) {
        return ApiResponse.<ConversationSummaryResponse>builder()
                .code(ChatResponseCode.UPDATE_CONVERSATION_SUCCESS.getCode())
                .message(ChatResponseCode.UPDATE_CONVERSATION_SUCCESS.getMessage())
                .data(conversationService.updateConversation(conversationId, request))
                .build();
    }

    @PostMapping("/{conversationId}/members")
    public ApiResponse<ConversationDetailResponse> addMembers(@PathVariable UUID conversationId, @RequestBody AddMembersRequest request) {
        return ApiResponse.<ConversationDetailResponse>builder()
                .code(ChatResponseCode.ADD_MEMBER_SUCCESS.getCode())
                .message(ChatResponseCode.ADD_MEMBER_SUCCESS.getMessage())
                .data(conversationService.addMembers(conversationId, request))
                .build();
    }

    @PatchMapping("/{conversationId}/members/{memberId}/status")
    public ApiResponse<ConversationDetailResponse> updateMemberRole(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberId,
            @RequestBody UpdateMemberRoleRequest request) {
        return ApiResponse.<ConversationDetailResponse>builder()
                .code(ChatResponseCode.UPDATE_CONVERSATION_SUCCESS.getCode())
                .message(ChatResponseCode.UPDATE_CONVERSATION_SUCCESS.getMessage())
                .data(conversationService.updateMemberRole(conversationId, memberId, request))
                .build();
    }
}
