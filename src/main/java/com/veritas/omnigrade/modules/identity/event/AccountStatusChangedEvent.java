package com.veritas.omnigrade.modules.identity.event;

import com.veritas.omnigrade.modules.identity.enumType.AccountStatus;
import java.time.Instant;

/**
 * Event publish khi trạng thái account thay đổi (ACTIVE → SUSPENDED, PENDING → ACTIVE, v.v.).
 *
 * <p>organization module dùng để sync trạng thái Student/Examiner nếu cần (ví dụ: khóa quyền thi
 * khi account bị suspend).
 *
 * <p>Kafka topic: {@code identity.account.status-changed} Key: accountId (UUID string)
 */
public record AccountStatusChangedEvent(
    String accountId, AccountStatus previousStatus, AccountStatus newStatus, Instant occurredAt) {

  public static AccountStatusChangedEvent of(
      String accountId, AccountStatus previousStatus, AccountStatus newStatus) {
    return new AccountStatusChangedEvent(accountId, previousStatus, newStatus, Instant.now());
  }
}
