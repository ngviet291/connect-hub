package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.response.MentionResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.service.MentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MentionController {

    private final MentionService mentionService;

    //GET /api/v1/posts/{id}/mentions
     //Lấy danh sách người được mention trong bài đăng

    @GetMapping("/v1/posts/{id}/mentions")
    public ApiResponse<CursorResponse<MentionResponse>> getMentionsByPost(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<MentionResponse>>builder()
                .code(PostResponseCode.GET_POST_MENTIONS_SUCCESS.getCode())
                .message(PostResponseCode.GET_POST_MENTIONS_SUCCESS.getMessage())
                .data(mentionService.getMentionsByPost(id, cursor, limit))
                .build();
    }

    //GET /api/v1/users/me/mentions
     // Lấy danh sách bài đăng đang mention người dùng hiện tại

    @GetMapping("/v1/users/me/mentions")
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
