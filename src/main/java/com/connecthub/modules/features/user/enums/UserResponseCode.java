package com.connecthub.modules.features.user.enums;

import lombok.Getter;

@Getter
public enum UserResponseCode {
    GET_USER_SUCCESS(3000, "User retrieved successfully"),
    GET_USER_FOLLOWERS_SUCCESS(3001, "Followers retrieved successfully"),
    GET_USER_FOLLOWING_SUCCESS(3002, "Following retrieved successfully"),
    FOLLOW_USER_SUCCESS(3003, "User followed successfully"),
    UNFOLLOW_USER_SUCCESS(3004, "User unfollowed successfully"),
    UPDATE_USER_SUCCESS(3005, "User updated successfully"),
    UPDATE_USER_STATUS_SUCCESS(3006, "User status updated successfully"),
    GET_USER_STATS_SUCCESS(3007, "User stats retrieved successfully"),
    GET_ALL_USERS_SUCCESS(3008, "All users retrieved successfully");
    private final int code;
    private final String message;

    UserResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

