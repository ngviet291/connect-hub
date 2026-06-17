package com.connecthub.modules.features.user.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ChangePasswordService {
    private static final String TOKEN_IAT_AVAILABLE = "TOKEN_IAT_AVAILABLE:";

    private final RedisTemplate<String, String> redis;

    private static final Long TTL_IN_SECOND = 86400L; // 24 hours

    public ChangePasswordService(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void saveTokenInvalidationTimestamp(String username, String changedPasswordDate ) {
        redis.opsForValue().set(TOKEN_IAT_AVAILABLE + username ,changedPasswordDate, TTL_IN_SECOND, TimeUnit.SECONDS);
    }


    public boolean isTokenInvalidationTimestampExists(String username) {
        return Boolean.TRUE.equals(redis.hasKey(TOKEN_IAT_AVAILABLE + username));
    }

    public String getTokenInvalidationTimestamp(String username) {
        return redis.opsForValue().get(TOKEN_IAT_AVAILABLE + username);
    }


}
