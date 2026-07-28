package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.service.RepostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/posts/{postId}/reposts")
@RequiredArgsConstructor
public class RepostController {

    private final RepostService repostService;

    @PostMapping
    public ApiResponse<Boolean> toggleRepost(@PathVariable UUID postId) {
        boolean reposted = repostService.toggleRepost(postId);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.REPOST_SUCCESS.getCode())
                .message(reposted ? "Reposted successfully" : "Repost removed")
                .data(reposted)
                .build();
    }

    // GET /v1/posts/{postId}/reposts/me — mình đã repost bài này chưa
    @GetMapping("/me")
    public ApiResponse<Boolean> hasReposted(@PathVariable UUID postId) {
        boolean reposted = repostService.hasReposted(postId);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.REPOST_SUCCESS.getCode())
                .message("Repost status retrieved successfully")
                .data(reposted)
                .build();
    }
}
