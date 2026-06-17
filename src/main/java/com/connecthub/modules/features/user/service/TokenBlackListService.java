package com.connecthub.modules.features.user.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlackListService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final RedisTemplate<String, String> redis;

    public TokenBlackListService(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    // Store jit tokens and expiration times of blocked tokens
    public void blacklistToken(String jitToken, long ttlSeconds) {
        redis.opsForValue().set(BLACKLIST_PREFIX + jitToken, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jitToken) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jitToken));
    }

}
