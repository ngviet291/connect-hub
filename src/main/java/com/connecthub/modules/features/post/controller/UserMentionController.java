package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.service.MentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserMentionController {
    private final MentionService mentionService;

    @GetMapping("/me/mentions")
    public ApiResponse<CursorResponse<PostResponse>> getMyMentions(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_MY_MENTIONS_SUCCESS.getCode())
                .message(PostResponseCode.GET_MY_MENTIONS_SUCCESS.getMessage())
                .data(mentionService.getMyMentions(cursor, limit))
                .build();
    }
}