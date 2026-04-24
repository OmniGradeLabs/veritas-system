package com.veritas.omnigrade.modules.organization.service.impl;

import com.veritas.omnigrade.modules.identity.event.AccountCreatedEvent;
import com.veritas.omnigrade.modules.identity.event.AccountStatusChangedEvent;
import com.veritas.omnigrade.modules.organization.service.ProfileProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProfileProvisioningServiceImpl implements ProfileProvisioningService {

  private static final Logger log = LoggerFactory.getLogger(ProfileProvisioningServiceImpl.class);

  // TODO: inject StudentRepository và ExaminerRepository khi tạo repository layer
  // private final StudentRepository studentRepository;
  // private final ExaminerRepository examinerRepository;

  @Override
  public void provisionProfile(AccountCreatedEvent event) {
    // TODO: implement khi có StudentRepository và ExaminerRepository
    // Bước 1: Kiểm tra idempotency — profile đã tồn tại chưa?
    // Bước 2: Dựa vào event.initialRoleName() quyết định tạo Student hay Examiner
    log.info(
        "[Organization] Provisioning profile for accountId={} role={}",
        event.accountId(),
        event.initialRoleName());
  }

  @Override
  public void syncProfileStatus(AccountStatusChangedEvent event) {
    // TODO: implement sau
    log.info(
        "[Organization] Syncing profile status for accountId={} newStatus={}",
        event.accountId(),
        event.newStatus());
  }
}
