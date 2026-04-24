package com.veritas.omnigrade.common.security.config;

import com.veritas.omnigrade.common.exception.ApiException;
import com.veritas.omnigrade.common.exception.ErrorCode;
import com.veritas.omnigrade.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.veritas.omnigrade.modules.identity.repository.SessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SessionGuardService {
  SessionRepository sessionRepository;
  SessionAuthorityCacheService sessionAuthorityCacheService;

  static final Duration ACTIVE_TTL = Duration.ofSeconds(60);

  public void ensureActive(UUID sessionId, Duration jwtTtl) {
    if (sessionId == null) throw new ApiException(ErrorCode.UNAUTHENTICATED);
    Duration safeTtl =
        (jwtTtl == null || jwtTtl.isNegative() || jwtTtl.isZero()) ? Duration.ofSeconds(1) : jwtTtl;

    // Tier 1: Check Revoked in Redis (O(1))
    if (sessionAuthorityCacheService.isRevoked(sessionId)) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    // Tier 2: Check Active in Redis
    if (sessionAuthorityCacheService.isActive(sessionId)) {
      return;
    }

    // Tier 3: Fallback DB
    boolean active = sessionRepository.isSessionActive(sessionId, LocalDateTime.now());
    if (!active) {
      sessionAuthorityCacheService.markRevoked(sessionId, safeTtl);
      sessionAuthorityCacheService.clearActive(sessionId);
      sessionAuthorityCacheService.clearAuthz(sessionId);
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    sessionAuthorityCacheService.markActive(sessionId, ACTIVE_TTL);
  }
}
