package com.connecthub.modules.features.user.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.social.dto.FollowStats;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.FollowResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.enums.UserResponseCode;
import com.connecthub.modules.features.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    // ADMIN ONLY
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.GET_USER_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_SUCCESS.getMessage())
                .data(userService.getUserById(id))
                .build();
    }
    @GetMapping
    public ApiResponse<UserResponse> getUser() {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.GET_USER_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_SUCCESS.getMessage())
                .data(userService.getUserById())
                .build();
    }
    // ADMIN
    @GetMapping("/{id}/followers")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowers(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(3) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_USER_FOLLOWERS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_FOLLOWERS_SUCCESS.getMessage())
                .data(userService.getFollowers(id, cursor, size))
                .build();
    }
    // USER
    @GetMapping("/followers")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowers(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(3) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_USER_FOLLOWERS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_FOLLOWERS_SUCCESS.getMessage())
                .data(userService.getFollowers(cursor, size))
                .build();
    }

    // ADMIN
    @GetMapping("/{id}/following")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowing(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_USER_FOLLOWING_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_FOLLOWING_SUCCESS.getMessage())
                .data(userService.getFollowing(id, cursor, size))
                .build();
    }
    // USER
    @GetMapping("/following")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getFollowing(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_USER_FOLLOWING_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_FOLLOWING_SUCCESS.getMessage())
                .data(userService.getFollowing(cursor, size))
                .build();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/follow")
    public ApiResponse<FollowResponse> follow(@PathVariable UUID id) {
        return ApiResponse.<FollowResponse>builder()
                .code(UserResponseCode.FOLLOW_USER_SUCCESS.getCode())
                .message(UserResponseCode.FOLLOW_USER_SUCCESS.getMessage())
                .data(userService.followUser(id))
                .build();
    }

    @DeleteMapping("/{id}/unfollow")
    public ApiResponse<FollowResponse> unfollow(@PathVariable UUID id) {
        return ApiResponse.<FollowResponse>builder()
                .code(UserResponseCode.UNFOLLOW_USER_SUCCESS.getCode())
                .message(UserResponseCode.UNFOLLOW_USER_SUCCESS.getMessage())
                .data(userService.unfollowUser(id))
                .build();
    }

    @PutMapping
    public ApiResponse<UserResponse> updateUser(

            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.UPDATE_USER_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_SUCCESS.getMessage())
                .data(userService.updateUser(request))
                .build();
    }

    @PutMapping(value = "/avatar", consumes = "multipart/form-data")
    public ApiResponse<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.UPDATE_USER_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_SUCCESS.getMessage())
                .data(userService.uploadAvatar(file))
                .build();
    }

    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(
            @Valid @RequestBody UserStatusRequest request
    ) {
        userService.changeStatus(request);
        return ApiResponse.<Void>builder()
                .code(UserResponseCode.UPDATE_USER_STATUS_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_STATUS_SUCCESS.getMessage())
                .build();
    }

    @GetMapping("/stats")
    public ApiResponse<FollowStats> getStats() {
        return ApiResponse.<FollowStats>builder()
                .code(UserResponseCode.GET_USER_STATS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_STATS_SUCCESS.getMessage())
                .data(userService.getStats())
                .build();
    }

    // ADMIN
    @GetMapping("/{id}/stats")
    public ApiResponse<FollowStats> getStats(@PathVariable UUID id) {
        return ApiResponse.<FollowStats>builder()
                .code(UserResponseCode.GET_USER_STATS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_STATS_SUCCESS.getMessage())
                .data(userService.getStats(id))
                .build();
    }

}
