    package com.connecthub.modules.features.search.controller;

    import com.connecthub.common.dto.response.ApiResponse;
    import com.connecthub.common.dto.response.CursorResponse;
    import com.connecthub.modules.features.post.dto.response.PostResponse;
    import com.connecthub.modules.features.search.dto.response.HashtagSearchResponse;
    import com.connecthub.modules.features.search.enums.SearchResponseCode;
    import com.connecthub.modules.features.search.service.SearchService;
    import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
    import lombok.RequiredArgsConstructor;
    import org.springframework.web.bind.annotation.*;

    import java.util.UUID;

    @RestController
    @RequestMapping("/v1/search")
    @RequiredArgsConstructor
    public class SearchController {

        private final SearchService searchService;

        /**
         * GET /api/v1/search/users?keyword=john&cursor=xxx&limit=20
         * Tìm kiếm người dùng theo tên / username
         */
        @GetMapping("/users")
        public ApiResponse<CursorResponse<UserSummaryResponse>> searchUsers(
                @RequestParam String keyword,
                @RequestParam(required = false) UUID cursor,
                @RequestParam(defaultValue = "20") int limit) {
            return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                    .code(SearchResponseCode.SEARCH_USERS_SUCCESS.getCode())
                    .message(SearchResponseCode.SEARCH_USERS_SUCCESS.getMessage())
                    .data(searchService.searchUsers(keyword, cursor, limit))
                    .build();
        }

        /**
         * GET /api/v1/search/posts?keyword=spring&cursor=xxx&limit=20
         * Tìm kiếm bài đăng theo từ khoá trong nội dung
         */
        @GetMapping("/posts")
        public ApiResponse<CursorResponse<PostResponse>> searchPosts(
                @RequestParam String keyword,
                @RequestParam(required = false) UUID cursor,
                @RequestParam(defaultValue = "20") int limit) {
            return ApiResponse.<CursorResponse<PostResponse>>builder()
                    .code(SearchResponseCode.SEARCH_POSTS_SUCCESS.getCode())
                    .message(SearchResponseCode.SEARCH_POSTS_SUCCESS.getMessage())
                    .data(searchService.searchPosts(keyword, cursor, limit))
                    .build();
        }

        /**
         * GET /api/v1/search/hashtags?keyword=fun&cursor=xxx&limit=20
         * Tìm kiếm hashtag theo tên
         */
        @GetMapping("/hashtags")
        public ApiResponse<CursorResponse<HashtagSearchResponse>> searchHashtags(
                @RequestParam String keyword,
                @RequestParam(required = false) UUID cursor,
                @RequestParam(defaultValue = "20") int limit) {
            return ApiResponse.<CursorResponse<HashtagSearchResponse>>builder()
                    .code(SearchResponseCode.SEARCH_HASHTAGS_SUCCESS.getCode())
                    .message(SearchResponseCode.SEARCH_HASHTAGS_SUCCESS.getMessage())
                    .data(searchService.searchHashtags(keyword, cursor, limit))
                    .build();
        }
    }
