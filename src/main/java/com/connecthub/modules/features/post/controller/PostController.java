package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.request.UpdatePostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.post.service.BookmarkService;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.post.service.RepostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostResponse> createPost(@ModelAttribute PostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.CREATE_POST_SUCCESS.getCode())
                .message(PostResponseCode.CREATE_POST_SUCCESS.getMessage())
                .data(postService.createPost(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable UUID id) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.GET_POST_SUCCESS.getCode())
                .message(PostResponseCode.GET_POST_SUCCESS.getMessage())
                .data(postService.getPost(id))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(@PathVariable UUID id,
                                                @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.UPDATE_POST_SUCCESS.getCode())
                .message(PostResponseCode.UPDATE_POST_SUCCESS.getMessage())
                .data(postService.updatePost(id, request))
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
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_FEED_SUCCESS.getCode())
                .message(PostResponseCode.GET_FEED_SUCCESS.getMessage())
                .data(postService.getUserFeed(cursor, limit))
                .build();
    }

    @GetMapping("/hashtags/{tag}")
    public ApiResponse<CursorResponse<PostResponse>> getPostsByHashtag(
            @PathVariable String tag,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_HASHTAG_POSTS_SUCCESS.getCode())
                .message(PostResponseCode.GET_HASHTAG_POSTS_SUCCESS.getMessage())
                .data(postService.getPostsByHashtag(tag, cursor, limit))
                .build();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/{id}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostResponse> createReply(@PathVariable UUID id,
                                                 @ModelAttribute PostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.CREATE_REPLY_SUCCESS.getCode())
                .message(PostResponseCode.CREATE_REPLY_SUCCESS.getMessage())
                .data(postService.createReply(id, request))
                .build();
    }

    @GetMapping("/{id}/replies")
    public ApiResponse<CursorResponse<PostResponse>> getReplies(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_REPLIES_SUCCESS.getCode())
                .message(PostResponseCode.GET_REPLIES_SUCCESS.getMessage())
                .data(postService.getReplies(id, cursor, limit))
                .build();
    }
}