package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.web.ApiException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();
    private final CampusGuardProperties properties;

    public RateLimitService(CampusGuardProperties properties) {
        this.properties = properties;
    }

    public void checkLogin(String key) {
        hit("login:" + key, properties.getRateLimit().getLoginPerMinute());
    }

    public void checkVerify(String key) {
        hit("verify:" + key, properties.getRateLimit().getVerifyPerMinute());
    }

    private void hit(String key, int perMinute) {
        Instant cutoff = Instant.now().minusSeconds(60);
        Deque<Instant> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
                q.removeFirst();
            }
            if (q.size() >= perMinute) {
                throw ApiException.tooMany("Too many attempts. Try again shortly.");
            }
            q.addLast(Instant.now());
        }
    }
}
