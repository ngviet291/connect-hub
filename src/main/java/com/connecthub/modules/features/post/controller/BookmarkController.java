package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.enums.PostResponseCode;
import com.connecthub.modules.features.post.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // POST /api/v1/posts/{id}/bookmarks
    // Thêm hoặc hủy bookmark bài đăng
    @PostMapping("/{id}/bookmarks")
    public ApiResponse<Boolean> toggleBookmark(@PathVariable UUID id) {
        boolean bookmarked = bookmarkService.toggleBookmark(id);

        return ApiResponse.<Boolean>builder()
                .code(bookmarked
                        ? PostResponseCode.BOOKMARK_SUCCESS.getCode()
                        : PostResponseCode.UNBOOKMARK_SUCCESS.getCode())
                .message(bookmarked
                        ? PostResponseCode.BOOKMARK_SUCCESS.getMessage()
                        : PostResponseCode.UNBOOKMARK_SUCCESS.getMessage())
                .data(bookmarked)
                .build();
    }

    // GET /api/v1/posts/bookmarks
    // Lấy danh sách bài đăng đã bookmark
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
}