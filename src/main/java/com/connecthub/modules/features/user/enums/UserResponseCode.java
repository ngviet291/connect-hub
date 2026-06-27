package com.connecthub.modules.features.user.enums;

import lombok.Getter;

@Getter
public enum UserResponseCode {
    GET_USER_SUCCESS(3000, "success.user.get_user"),
    GET_USER_FOLLOWERS_SUCCESS(3001, "success.user.get_followers"),
    GET_USER_FOLLOWING_SUCCESS(3002, "success.user.get_following"),
    FOLLOW_USER_SUCCESS(3003, "success.user.follow"),
    UNFOLLOW_USER_SUCCESS(3004, "success.user.unfollow"),
    UPDATE_USER_SUCCESS(3005, "success.user.update"),
    UPDATE_USER_STATUS_SUCCESS(3006, "success.user.update_status"),
    GET_USER_STATS_SUCCESS(3007, "success.user.get_stats"),
    BLOCK_USER_SUCCESS(3009, "success.user.block"),
    UNBLOCK_USER_SUCCESS(3010, "success.user.unblock"),
    GET_BLOCKED_USERS_SUCCESS(3011, "success.user.get_blocked_users"),
    GET_BLOCK_STATUS_SUCCESS(3012, "success.user.get_block_status"),
    GET_ALL_USERS_SUCCESS(3008, "success.user.get_all_users");
    private final int code;
    private final String message;

    UserResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}