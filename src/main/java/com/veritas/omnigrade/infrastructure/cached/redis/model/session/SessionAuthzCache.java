package com.veritas.omnigrade.infrastructure.cached.redis.model.session;

import java.util.List;
import java.util.UUID;

// This define the value of StringRedisTemplate<Key, Value>
// The roles and permissions are String not Entity for the minimal purpose, less coupling with DB
// Immutable representation of a user's authorization context for caching. Decouples security
public record SessionAuthzCache(
    UUID userId,
    UUID universityId,
    List<String> roles,
    List<String> groups,
    List<String> permissions) {}
