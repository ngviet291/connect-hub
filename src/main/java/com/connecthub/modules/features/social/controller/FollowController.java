package com.connecthub.modules.features.social.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.social.enums.FollowResponseCode;
import com.connecthub.modules.features.social.service.FollowService;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/follow")
public class FollowController {
    private final FollowService followService;

    @GetMapping("/{username}/following")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowing(
            @PathVariable String username,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(FollowResponseCode.GET_FOLLOWING_SUCCESS.getCode())
                .message(FollowResponseCode.GET_FOLLOWING_SUCCESS.getMessage())
                .data(followService.getFollowing(username, cursor, size))
                .build();
    }

    @GetMapping("/{username}/followers")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowers(
            @PathVariable String username,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(FollowResponseCode.GET_FOLLOWERS_SUCCESS.getCode())
                .message(FollowResponseCode.GET_FOLLOWERS_SUCCESS.getMessage())
                .data(followService.getFollowers(username, cursor, size))
                .build();
    }
}
