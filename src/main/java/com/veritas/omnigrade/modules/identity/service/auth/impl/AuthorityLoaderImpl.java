package com.veritas.omnigrade.modules.identity.service.auth.impl;

import com.veritas.omnigrade.common.exception.ApiException;
import com.veritas.omnigrade.common.exception.ErrorCode;
import com.veritas.omnigrade.infrastructure.cached.redis.model.session.SessionAuthzCache;
import com.veritas.omnigrade.modules.identity.repository.AccountRepository;
import com.veritas.omnigrade.modules.identity.repository.GroupRepository;
import com.veritas.omnigrade.modules.identity.repository.PermissionRepository;
import com.veritas.omnigrade.modules.identity.service.auth.AuthorityLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthorityLoaderImpl implements AuthorityLoader {

  AccountRepository accountRepository;
  GroupRepository groupRepository;
  PermissionRepository permissionRepository;

  @Override
  @Transactional(readOnly = true)
  public SessionAuthzCache load(UUID accountId) {

    var coreInfo =
        accountRepository
            .findCoreAuthInfoById(accountId)
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_USER_ID));

    List<String> roles = new ArrayList<>();
    roles.add(coreInfo.getRole());

    List<String> groups = groupRepository.findGroupNamesByAccountId(accountId);
    if (groups == null) groups = new ArrayList<>();

    List<String> permissions = permissionRepository.findPermissionNamesByAccountId(accountId);
    if (permissions == null) permissions = new ArrayList<>();

    return new SessionAuthzCache(accountId, coreInfo.getUniversityId(), roles, groups, permissions);
  }
}
