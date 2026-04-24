package com.veritas.omnigrade.infrastructure.cached.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.omnigrade.infrastructure.cached.redis.helper.RedisJsonCacheSupport;
import com.veritas.omnigrade.infrastructure.cached.redis.keys.RedisKeys;
import com.veritas.omnigrade.infrastructure.cached.redis.model.session.SessionAuthzCache;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthorityCacheService extends RedisJsonCacheSupport {

  public SessionAuthorityCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
    super(redis, objectMapper);
  }

  public Optional<SessionAuthzCache> get(UUID sessionId) {
    if (sessionId == null) return Optional.empty();
    return readRawValue(RedisKeys.sessionAuthz(sessionId))
        .flatMap(json -> deserialize(json, SessionAuthzCache.class));
  }

  public boolean put(UUID sessionId, SessionAuthzCache value, Duration ttl) {
    if (sessionId == null || value == null || !isValidTtl(ttl)) return false;
    return serialize(value)
        .map(json -> writeValue(RedisKeys.sessionAuthz(sessionId), json, ttl))
        .orElse(false);
  }

  public void markRevoked(UUID sessionId, Duration ttl) {
    if (sessionId != null && isValidTtl(ttl)) {
      writeValue(RedisKeys.sessionRevoked(sessionId), "1", ttl);
    }
  }

  public boolean isRevoked(UUID sessionId) {
    if (sessionId == null) return false;
    return hasKey(RedisKeys.sessionRevoked(sessionId));
  }

  public void markActive(UUID sessionId, Duration ttl) {
    if (sessionId != null && isValidTtl(ttl)) {
      writeValue(RedisKeys.sessionActive(sessionId), "1", ttl);
    }
  }

  public boolean isActive(UUID sessionId) {
    if (sessionId == null) return false;
    return hasKey(RedisKeys.sessionActive(sessionId));
  }

  public void clearActive(UUID sessionId) {
    if (sessionId != null) deleteKey(RedisKeys.sessionActive(sessionId));
  }

  public void clearAuthz(UUID sessionId) {
    if (sessionId != null) deleteKey(RedisKeys.sessionAuthz(sessionId));
  }
}
