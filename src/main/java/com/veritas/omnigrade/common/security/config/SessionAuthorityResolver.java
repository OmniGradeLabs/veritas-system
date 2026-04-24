package com.veritas.omnigrade.common.security.config;

import com.veritas.omnigrade.common.exception.ApiException;
import com.veritas.omnigrade.infrastructure.cached.redis.model.session.SessionAuthzCache;
import com.veritas.omnigrade.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.veritas.omnigrade.modules.identity.service.auth.AuthorityLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SessionAuthorityResolver {
  SessionAuthorityCacheService sessionAuthorityCacheService;
  AuthorityLoader authorityLoader;
  SessionGuardService sessionGuardService;

  public SessionAuthzCache resolve(UUID sessionId, UUID userId, Instant jwtExp) {
    Duration ttl = Duration.between(Instant.now(), jwtExp);
    if (ttl.isNegative() || ttl.isZero()) ttl = Duration.ofSeconds(1);

    sessionGuardService.ensureActive(sessionId, ttl);

    try {
      SessionAuthzCache cached = sessionAuthorityCacheService.get(sessionId).orElse(null);
      if (cached != null) return cached;

      SessionAuthzCache loaded = authorityLoader.load(userId);
      sessionAuthorityCacheService.put(sessionId, loaded, ttl);
      return loaded;
    } catch (ApiException e) {
      throw e;
    } catch (Exception ex) {
      log.warn("AUTHZ cache FAIL -> fallback DB sessionId={} userId={}.", sessionId, userId, ex);
      return authorityLoader.load(userId);
    }
  }
}
