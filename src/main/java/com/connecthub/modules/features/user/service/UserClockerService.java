package com.connecthub.modules.features.user.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserClockerService {
    private static final String USER_CLOCKER_PREFIX = "login:failed:";
    private final RedisTemplate<String, String> redis;

    private final int TIMEOUT_MINUTES = 15;

    public UserClockerService(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }


    public int incrementFailedAttempts(String username) {

        String key = getKey(username);

        Long count = redis.opsForValue().increment(key);

        if (count == 1) {
            redis.expire(key, TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }

        return count.intValue();
    }

    public String getKey(String username) {
        return USER_CLOCKER_PREFIX + username;
    }

    public void resetFailedAttempts(String username) {
        redis.delete(getKey(username));
    }




}
