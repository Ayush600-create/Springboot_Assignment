package com.grid07.socialmedia.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Increment a key by 1 and return new value
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // Check if a key exists
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Set a key with TTL (expiry time)
    public void setWithTTL(String key, String value, long minutes) {
        redisTemplate.opsForValue().set(key, value, minutes, TimeUnit.MINUTES);
    }

    // Get value of a key
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Push to a Redis List
    public void pushToList(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    // Pop all items from a Redis List
    public java.util.List<String> popAllFromList(String key) {
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) return new java.util.ArrayList<>();
        return redisTemplate.opsForList().leftPop(key, size);
    }

    // Delete a key
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}