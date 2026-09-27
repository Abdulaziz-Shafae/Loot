package com.example.loot.Security;

import org.springframework.stereotype.Component;
import java.util.*;

/** Bounded, single-instance fixed-window limiter. Use a shared store when scaling horizontally. */
@Component
public class RateLimits {
    private record Bucket(long expires, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    public synchronized boolean allow(String key, int limit, long seconds) {
        long now = System.currentTimeMillis();
        if (buckets.size() > 5000) buckets.entrySet().removeIf(e -> e.getValue().expires < now);
        var b = buckets.get(key);
        if (b == null || b.expires < now) {
            if (buckets.size() >= 10000) return false;
            buckets.put(key, new Bucket(now + seconds * 1000, 1)); return true;
        }
        if (b.count >= limit) return false;
        buckets.put(key, new Bucket(b.expires, b.count + 1)); return true;
    }
}
