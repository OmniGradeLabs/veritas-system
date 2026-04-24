package com.veritas.omnigrade.modules.identity.event;

import com.veritas.omnigrade.modules.identity.enumType.AccountStatus;
import java.time.Instant;

/**
 * Event publish khi account mới được tạo thành công.
 *
 * <p>organization module lắng nghe event này để tự động tạo Student hoặc Examiner profile tương ứng
 * (dựa vào role).
 *
 * <p>Kafka topic: {@code identity.account.created} Key: accountId (UUID string)
 */
public record AccountCreatedEvent(
    String accountId,
    String universityId,
    String username,
    String email,
    AccountStatus status,
    /** Role ban đầu của account — organization dùng để quyết định tạo Student hay Examiner. */
    String initialRoleName,
    Instant occurredAt) {

  public static AccountCreatedEvent of(
      String accountId,
      String universityId,
      String username,
      String email,
      AccountStatus status,
      String initialRoleName) {
    return new AccountCreatedEvent(
        accountId, universityId, username, email, status, initialRoleName, Instant.now());
  }
}
