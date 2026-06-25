package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.PostViewResponse;
import com.connecthub.modules.features.post.dto.response.ReactionCountResponse;
import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.service.BookmarkService;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.post.service.PostViewService;
import com.connecthub.modules.features.post.service.ReactionService;
import com.connecthub.modules.features.post.service.RepostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ReactionService reactionService;
    private final BookmarkService bookmarkService;
    private final RepostService repostService;
    private final PostViewService postViewService;


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
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

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(@PathVariable UUID id,
                                                @Valid @RequestBody PostRequest request) {
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


    @PostMapping("/{id}/hashtags")
    public ApiResponse<Void> addHashtagToPost(@PathVariable UUID id,
                                              @RequestParam String hashtag) {
        postService.addHashtagToPost(id, hashtag);
        return ApiResponse.<Void>builder()
                .code(PostResponseCode.ADD_HASHTAG_SUCCESS.getCode())
                .message(PostResponseCode.ADD_HASHTAG_SUCCESS.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/replies")
    public ApiResponse<PostResponse> createReply(@PathVariable UUID id,
                                                 @Valid @RequestBody PostRequest request) {
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

    // Toggle like/unlike — gọi 2 lần để bỏ like
    @PostMapping("/{id}/reactions")
    public ApiResponse<Boolean> toggleReaction(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "LIKE") ReactionType type) {
        boolean reacted = reactionService.toggleReaction(id, type);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message(reacted ? "Reacted successfully" : "Reaction removed")
                .data(reacted)
                .build();
    }

    // Toggle bookmark/unbookmark — gọi 2 lần để bỏ bookmark
    @PostMapping("/{id}/bookmarks")
    public ApiResponse<Boolean> toggleBookmark(@PathVariable UUID id) {
        boolean bookmarked = bookmarkService.toggleBookmark(id);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.BOOKMARK_SUCCESS.getCode())
                .message(bookmarked ? "Bookmarked successfully" : "Bookmark removed")
                .data(bookmarked)
                .build();
    }

    // GET /v1/posts/bookmarks?cursor=xxx&limit=20 - get bookmarked posts của user hiện tại
    @GetMapping("/bookmarks")
    public ApiResponse<CursorResponse<PostResponse>> getBookmarkedPosts(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_BOOKMARKS_SUCCESS.getCode())
                .message(PostResponseCode.GET_BOOKMARKS_SUCCESS.getMessage())
                .data(bookmarkService.getBookmarkedPosts(cursor, limit))
                .build();
    }
    // Toggle repost/unrepost — gọi 2 lần để bỏ repost
    @PostMapping("/{id}/reposts")
    public ApiResponse<Boolean> toggleRepost(@PathVariable UUID id) {
        boolean reposted = repostService.toggleRepost(id);
        return ApiResponse.<Boolean>builder()
                .code(PostResponseCode.REPOST_SUCCESS.getCode())
                .message(reposted ? "Reposted successfully" : "Repost removed")
                .data(reposted)
                .build();
    }

    //record view khi user xem bài viết (có thể gọi từ frontend khi mở bài viết)
    @PostMapping("/{id}/views")
    public ApiResponse<Void> recordView(@PathVariable UUID id) {
        postViewService.recordView(id);
        return ApiResponse.<Void>builder()
                .code(PostResponseCode.VIEW_RECORDED_SUCCESS.getCode())
                .message(PostResponseCode.VIEW_RECORDED_SUCCESS.getMessage())
                .build();
    }

    // GET /v1/posts/{id}/reactions?cursor=xxx&limit=20
    // Lấy danh sách người đã react bài đăng (cursor-based pagination)
    @GetMapping("/{id}/reactions")
    public ApiResponse<CursorResponse<ReactionResponse>> getReactions(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<CursorResponse<ReactionResponse>>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message("Reactions retrieved successfully")
                .data(reactionService.getReactionsByPost(id, cursor, limit))
                .build();
    }

    // GET /v1/posts/{id}/reactions/count
    // Đếm số lượng react theo từng loại (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)
    @GetMapping("/{id}/reactions/count")
    public ApiResponse<List<ReactionCountResponse>> countReactions(@PathVariable UUID id) {
        return ApiResponse.<List<ReactionCountResponse>>builder()
                .code(PostResponseCode.REACTION_SUCCESS.getCode())
                .message("Reaction counts retrieved successfully")
                .data(reactionService.countReactionsByType(id))
                .build();
    }
    // GET /v1/posts/{id}/viewsCount
    // Lấy số lượng view của bài viết
    @GetMapping("/{id}/viewsCount")
    public ApiResponse<PostViewResponse> getViewCount(@PathVariable UUID id) {
        return ApiResponse.<PostViewResponse>builder()
                .message("View count retrieved successfully")
                .data(
                        PostViewResponse.builder()
                                .viewCount(postViewService.getViewCount(id))
                                .build()
                )
                .build();
    }
}