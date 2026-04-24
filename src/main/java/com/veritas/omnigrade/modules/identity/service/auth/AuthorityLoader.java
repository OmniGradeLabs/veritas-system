package com.veritas.omnigrade.modules.identity.service.auth;

import com.veritas.omnigrade.infrastructure.cached.redis.model.session.SessionAuthzCache;
import java.util.UUID;

public interface AuthorityLoader {
  SessionAuthzCache load(UUID accountId);
}
