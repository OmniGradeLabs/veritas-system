package com.veritas.omnigrade.modules.identity.dto.auth.projection;

import java.util.UUID;

public interface AccountCoreAuthProjection {
  UUID getUniversityId();

  String getRole();
}
