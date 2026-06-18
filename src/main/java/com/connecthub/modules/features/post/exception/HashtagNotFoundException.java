package com.connecthub.modules.features.post.exception;

public class HashtagNotFoundException extends RuntimeException {
    public HashtagNotFoundException() {
        super("Hashtag not found");
    }
}