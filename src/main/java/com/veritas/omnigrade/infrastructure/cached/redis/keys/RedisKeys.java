package com.veritas.omnigrade.infrastructure.cached.redis.keys;

import java.util.Objects;
import java.util.UUID;

// This file define of key in StringRedisTemplate<Key,Value>
public final class RedisKeys {
  private RedisKeys() {}

  private static final String PREFIX = "omnigrade";

  // cached role and permission
  private static final String SESS_AUTHZ = PREFIX + ":sess:authz:";

  // kill access immediatly when session is revoke
  // if key exist --> 401
  private static final String SESS_REVOKED = PREFIX + ":sess:revoked:";
  // avoid to hit DB to check whether session is active or not
  // if key exist --> session active
  private static final String SESS_ACTIVE = PREFIX + ":sess:active:";

  // cached for permission tree
  private static final String PERM_TREE = PREFIX + ":perm:tree:";
  private static final String PERM_TREE_GROUP = PREFIX + ":perm:tree:group:";

  private static final String RATE_LIMIT_USER = PREFIX + ":ratelimit:user:";
  private static final String RATE_LIMIT_LOGIN = PREFIX + ":ratelimit:login:";

  public static String sessionRevoked(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_REVOKED + sessionId;
  }

  public static String sessionAuthz(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_AUTHZ + sessionId;
  }

  public static String sessionActive(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_ACTIVE + sessionId;
  }

  public static String permissionTreeGroup(UUID groupId) {
    requireNonBlank(groupId, "groupId");
    return PERM_TREE_GROUP + groupId;
  }

  // this permission do not related to any entity
  public static String permissionTree() {
    return PERM_TREE;
  }

  private static void requireNonBlank(UUID value, String name) {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException(name + "must not be blank");
    }
  }

  public static String getChatKey(String conversationId) {
    return PREFIX + ":chat:latest:" + conversationId;
  }

  public static String rateLimitUser(UUID userId) {
    requireNonBlank(userId, "userId");
    return RATE_LIMIT_USER + userId;
  }

  public static String rateLimitLogin(String phoneNumber) {
    return RATE_LIMIT_LOGIN + phoneNumber;
  }
}
