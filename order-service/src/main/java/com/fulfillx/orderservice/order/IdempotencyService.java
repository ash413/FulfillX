package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "idem:order:";
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(60);
    private static final Duration RESULT_TTL = Duration.ofHours(24);

    public enum Outcome { STARTED, REPLAY, IN_PROGRESS, MISMATCH }

    public record Begin(Outcome outcome, OrderResponse replay) { }

    public record Entry(String fingerprint, String state, OrderResponse response) { }

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    private String redisKey(Long userId, String key) {
        return PREFIX + userId + ":" + key;
    }

    public String fingerprint(CreateOrderRequest request) {
        return jsonMapper.writeValueAsString(request);
    }

    public Begin begin(Long userId, String key, String fingerprint) {
        String redisKey = redisKey(userId, key);
        String inProgress = jsonMapper.writeValueAsString(new Entry(fingerprint, "IN_PROGRESS", null));

        for (int attempt = 0; attempt < 3; attempt++) {
            Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, inProgress, IN_PROGRESS_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                return new Begin(Outcome.STARTED, null);
            }

            String existing = redis.opsForValue().get(redisKey);
            if (existing == null) {
                continue;   // it expired between the two calls: try to claim it again
            }

            Entry entry = jsonMapper.readValue(existing, Entry.class);
            if (!entry.fingerprint().equals(fingerprint)) {
                return new Begin(Outcome.MISMATCH, null);
            }
            if ("DONE".equals(entry.state())) {
                return new Begin(Outcome.REPLAY, entry.response());
            }
            return new Begin(Outcome.IN_PROGRESS, null);
        }
        return new Begin(Outcome.IN_PROGRESS, null);
    }

    public void complete(Long userId, String key, String fingerprint, OrderResponse response) {
        String json = jsonMapper.writeValueAsString(new Entry(fingerprint, "DONE", response));
        redis.opsForValue().set(redisKey(userId, key), json, RESULT_TTL);
    }

    public void abandon(Long userId, String key) {
        redis.delete(redisKey(userId, key));
    }
}