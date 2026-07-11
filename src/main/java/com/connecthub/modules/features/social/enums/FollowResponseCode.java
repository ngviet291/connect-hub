package com.connecthub.modules.features.social.enums;

import lombok.Getter;

@Getter
public enum FollowResponseCode {
    FOLLOW_SUCCESS("Followed successfully", 200),
    GET_FOLLOWING_SUCCESS("Successfully retrieved following users", 200),
    GET_FOLLOWERS_SUCCESS("Successfully retrieved followers", 200)

    ;

    private final String message;
    private final int code;

    FollowResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
