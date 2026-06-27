package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.response.ReactionCountResponse;
import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/posts/{postId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    // Toggle like/unlike — gọi 2 lần để bỏ like
    @PostMapping
    public ApiResponse<Boolean> toggleReaction(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "LIKE") ReactionType type) {
        boolean reacted = reactionService.toggleReaction(postId, type);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message(reacted ? "Reacted successfully" : "Reaction removed")
                .data(reacted)
                .build();
    }

    // GET /v1/posts/{postId}/reactions?cursor=xxx&limit=20
    @GetMapping
    public ApiResponse<CursorResponse<ReactionResponse>> getReactions(
            @PathVariable UUID postId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<ReactionResponse>>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message("Reactions retrieved successfully")
                .data(reactionService.getReactionsByPost(postId, cursor, limit))
                .build();
    }

    // GET /v1/posts/{postId}/reactions/count
    @GetMapping("/count")
    public ApiResponse<List<ReactionCountResponse>> countReactions(@PathVariable UUID postId) {
        return ApiResponse.<List<ReactionCountResponse>>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message("Reaction counts retrieved successfully")
                .data(reactionService.countReactionsByType(postId))
                .build();
    }
}
