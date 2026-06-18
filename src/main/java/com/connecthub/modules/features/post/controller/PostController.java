package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        PostResponse response = postService.createPost(request);
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.CREATE_POST_SUCCESS.getCode())
                .message(PostResponseCode.CREATE_POST_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable UUID id) {
        PostResponse response = postService.getPost(id);
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.GET_POST_SUCCESS.getCode())
                .message(PostResponseCode.GET_POST_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(@PathVariable UUID id, @Valid @RequestBody PostRequest request) {
        PostResponse response = postService.updatePost(id, request);
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.UPDATE_POST_SUCCESS.getCode())
                .message(PostResponseCode.UPDATE_POST_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable UUID id) {
        postService.deletePost(id);
        return ApiResponse.<Void>builder()
                .code(PostResponseCode.DELETE_POST_SUCCESS.getCode())
                .message(PostResponseCode.DELETE_POST_SUCCESS.getMessage())
                .build();
    }

    @GetMapping("/feed")
    public ApiResponse<CursorResponse<PostResponse>> getUserFeed(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        CursorResponse<PostResponse> response = postService.getUserFeed(cursor, limit);
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_FEED_SUCCESS.getCode())
                .message(PostResponseCode.GET_FEED_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @GetMapping("/hashtags/{tag}")
    public ApiResponse<CursorResponse<PostResponse>> getPostsByHashtag(
            @PathVariable String tag,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        CursorResponse<PostResponse> response = postService.getPostsByHashtag(tag, cursor, limit);
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_HASHTAG_POSTS_SUCCESS.getCode())
                .message(PostResponseCode.GET_HASHTAG_POSTS_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/{id}/hashtags")
    public ApiResponse<Void> addHashtagToPost(@PathVariable UUID id, @RequestParam String hashtag) {
        postService.addHashtagToPost(id, hashtag);
        return ApiResponse.<Void>builder()
                .code(PostResponseCode.ADD_HASHTAG_SUCCESS.getCode())
                .message(PostResponseCode.ADD_HASHTAG_SUCCESS.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/replies")
    public ApiResponse<PostResponse> createReply(@PathVariable UUID id, @Valid @RequestBody PostRequest request) {
        PostResponse response = postService.createReply(id, request);
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.CREATE_REPLY_SUCCESS.getCode())
                .message(PostResponseCode.CREATE_REPLY_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @GetMapping("/{id}/replies")
    public ApiResponse<CursorResponse<PostResponse>> getReplies(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        CursorResponse<PostResponse> response = postService.getReplies(id, cursor, limit);
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_REPLIES_SUCCESS.getCode())
                .message(PostResponseCode.GET_REPLIES_SUCCESS.getMessage())
                .data(response)
                .build();
    }
}