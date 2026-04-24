package com.veritas.omnigrade.modules.organization.service;

import com.veritas.omnigrade.modules.identity.event.AccountCreatedEvent;
import com.veritas.omnigrade.modules.identity.event.AccountStatusChangedEvent;

public interface ProfileProvisioningService {

  void provisionProfile(AccountCreatedEvent event);

  void syncProfileStatus(AccountStatusChangedEvent event);
}
