package com.connecthub.modules.features.user.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.modules.features.user.dto.response.FollowStatsResponse;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.FollowResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.BlockStatusResponse;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.enums.UserResponseCode;
import com.connecthub.modules.features.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
@RequestMapping("/v1")
public class UserController {

    private final UserService userService;

    // ADMIN ONLY
    @GetMapping("/admin/users/allusers")
    public ApiResponse<CursorResponse<UserSummaryResponse>> getAllUsers(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_ALL_USERS_SUCCESS.getCode())
                .message(UserResponseCode.GET_ALL_USERS_SUCCESS.getMessage())
                .data(userService.getAllUsers(cursor, size))
                .build();
    }
    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.GET_USER_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_SUCCESS.getMessage())
                .data(userService.getUserById(id))
                .build();
    }
    @GetMapping("/me")
    public ApiResponse<UserResponse> getProfile() {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.GET_USER_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_SUCCESS.getMessage())
                .data(userService.getProfile())
                .build();
    }
    // ADMIN
    @GetMapping("/admin/users/{id}/followers")
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
    @GetMapping("/users/followers")
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
    @GetMapping("/admin/users/{id}/following")
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
    @GetMapping("/users/following")
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
    @PostMapping("/users/{id}/follow")
    public ApiResponse<FollowResponse> follow(@PathVariable UUID id) {
        return ApiResponse.<FollowResponse>builder()
                .code(UserResponseCode.FOLLOW_USER_SUCCESS.getCode())
                .message(UserResponseCode.FOLLOW_USER_SUCCESS.getMessage())
                .data(userService.followUser(id))
                .build();
    }
    //users
    @DeleteMapping("/users/{id}/unfollow")
    public ApiResponse<FollowResponse> unfollow(@PathVariable UUID id) {
        return ApiResponse.<FollowResponse>builder()
                .code(UserResponseCode.UNFOLLOW_USER_SUCCESS.getCode())
                .message(UserResponseCode.UNFOLLOW_USER_SUCCESS.getMessage())
                .data(userService.unfollowUser(id))
                .build();
    }

    @PutMapping("/users")
    public ApiResponse<UserResponse> updateUser(
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.UPDATE_USER_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_SUCCESS.getMessage())
                .data(userService.updateUser(request))
                .build();
    }

    @PutMapping(value = "/users/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.UPDATE_USER_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_SUCCESS.getMessage())
                .data(userService.uploadAvatar(file))
                .build();
    }

    @PutMapping("/users/status")
    public ApiResponse<Void> changeStatus(
            @Valid @RequestBody UserStatusRequest request
    ) {
        userService.changeStatus(request);
        return ApiResponse.<Void>builder()
                .code(UserResponseCode.UPDATE_USER_STATUS_SUCCESS.getCode())
                .message(UserResponseCode.UPDATE_USER_STATUS_SUCCESS.getMessage())
                .build();
    }

    @GetMapping("/users/stats")
    public ApiResponse<FollowStatsResponse> getMyStats() {
        return ApiResponse.<FollowStatsResponse>builder()
                .code(UserResponseCode.GET_USER_STATS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_STATS_SUCCESS.getMessage())
                .data(userService.getMyStats())
                .build();
    }

    @GetMapping("/users/{id}/stats")
    public ApiResponse<FollowStatsResponse> getStats(@PathVariable UUID id) {
        return ApiResponse.<FollowStatsResponse>builder()
                .code(UserResponseCode.GET_USER_STATS_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_STATS_SUCCESS.getMessage())
                .data(userService.getStats(id))
                .build();
    }

    //User block
    // api này dùng để chặn người dùng
    @PostMapping("/users/{id}/block")
    public ApiResponse<UserResponse> blockUser(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.BLOCK_USER_SUCCESS.getCode())
                .message(UserResponseCode.BLOCK_USER_SUCCESS.getMessage())
                .data(userService.blockUser(id))
                .build();
    }
    //  api này dùng để bỏ chặn người dùng
    @DeleteMapping("/users/{id}/unblock")
    public ApiResponse<UserResponse> unblockUser(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.UNBLOCK_USER_SUCCESS.getCode())
                .message(UserResponseCode.UNBLOCK_USER_SUCCESS.getMessage())
                .data(userService.unblockUser(id))
                .build();
    }
    @GetMapping("/users/blocked")
    // lấy danh sách người dùng bị chặn bởi người dùng hiện tại
    public ApiResponse<CursorResponse<UserSummaryResponse>> getBlockedUsers(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.<CursorResponse<UserSummaryResponse>>builder()
                .code(UserResponseCode.GET_BLOCKED_USERS_SUCCESS.getCode())
                .message(UserResponseCode.GET_BLOCKED_USERS_SUCCESS.getMessage())
                .data(userService.getBlockedUsers(cursor, size))
                .build();
    }
    // kiểm tra trạng thái block user xem user block targetUser hay ko
    @GetMapping("/{id}/block/status")
    public ApiResponse<BlockStatusResponse> getBlockStatus(@PathVariable UUID id) {
        return ApiResponse.<BlockStatusResponse>builder()
                .code(UserResponseCode.GET_BLOCK_STATUS_SUCCESS.getCode())
                .message(UserResponseCode.GET_BLOCK_STATUS_SUCCESS.getMessage())
                .data(userService.isBlockingUser(id))
                .build();
    }

    @GetMapping("/users/username/{username}")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        return ApiResponse.<UserResponse>builder()
                .code(UserResponseCode.GET_USER_SUCCESS.getCode())
                .message(UserResponseCode.GET_USER_SUCCESS.getMessage())
                .data(userService.getUserByUsername(username))
                .build();
    }
}