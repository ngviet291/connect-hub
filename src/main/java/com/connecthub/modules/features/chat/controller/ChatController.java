package com.connecthub.modules.features.chat.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@RequestBody SendMessageRequest messageRequest){
        return ApiResponse.<MessageResponse>builder()
                .message("Message sent successfully")
                .data(chatService.sendFirstMessage(messageRequest))
                .code(200)
                .build();
    }

}
