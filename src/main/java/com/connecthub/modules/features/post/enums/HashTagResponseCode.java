package com.connecthub.modules.features.post.enums;

import lombok.Getter;

@Getter
public enum HashTagResponseCode {

    TRENDING_HASHTAG("Trending hashtags fetched successfully", 200),


    ;

    private final String message;
    private final int code;

    HashTagResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
