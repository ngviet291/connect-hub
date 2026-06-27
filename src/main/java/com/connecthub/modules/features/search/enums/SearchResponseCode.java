package com.connecthub.modules.features.search.enums;

import lombok.Getter;

@Getter
public enum SearchResponseCode {
    SEARCH_USERS_SUCCESS("success.search.users", 7000),
    SEARCH_POSTS_SUCCESS("success.search.posts", 7001),
    SEARCH_HASHTAGS_SUCCESS("success.search.hashtags", 7002);

    private final String message;
    private final int code;

    SearchResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
