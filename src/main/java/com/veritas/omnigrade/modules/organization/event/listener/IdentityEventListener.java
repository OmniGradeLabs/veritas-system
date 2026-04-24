package com.veritas.omnigrade.modules.organization.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.omnigrade.infrastructure.messaging.kafka.config.KafkaTopics;
import com.veritas.omnigrade.modules.identity.event.AccountCreatedEvent;
import com.veritas.omnigrade.modules.identity.event.AccountStatusChangedEvent;
import com.veritas.omnigrade.modules.organization.service.ProfileProvisioningService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe events từ identity module và xử lý phía organization.
 *
 * <p>Đây là điểm duy nhất trong organization biết về Kafka messages từ identity. Inject service nội
 * bộ — không inject thứ gì từ identity.
 *
 * <p>Consumer group: {@code organization-service} — mỗi module có group riêng để offset độc lập.
 */
@Component
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IdentityEventListener {

  static Logger log = LoggerFactory.getLogger(IdentityEventListener.class);

  ProfileProvisioningService profileProvisioningService;
  ObjectMapper kafkaObjectMapper;

  public IdentityEventListener(
      ProfileProvisioningService profileProvisioningService,
      @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
    this.profileProvisioningService = profileProvisioningService;
    this.kafkaObjectMapper = kafkaObjectMapper;
  }

  /**
   * Tạo Student hoặc Examiner profile khi Account mới được tạo trong identity.
   *
   * <p>Idempotency: service bên trong phải kiểm tra profile đã tồn tại chưa trước khi tạo mới
   * (tránh duplicate khi Kafka retry).
   */
  @KafkaListener(
      topics = KafkaTopics.IDENTITY_ACCOUNT_CREATED,
      groupId = "organization-service",
      containerFactory = "kafkaListenerContainerFactory")
  public void onAccountCreated(ConsumerRecord<String, Object> record, Acknowledgment ack) {
    try {
      AccountCreatedEvent event =
          kafkaObjectMapper.convertValue(record.value(), AccountCreatedEvent.class);

      log.info(
          "[Kafka] Received AccountCreatedEvent accountId={} role={}",
          event.accountId(),
          event.initialRoleName());

      profileProvisioningService.provisionProfile(event);

      ack.acknowledge(); // ACK sau khi xử lý thành công

    } catch (Exception ex) {
      log.error(
          "[Kafka] Failed to process AccountCreatedEvent key={} offset={}",
          record.key(),
          record.offset(),
          ex);
      // TODO:
      // Không ACK → Kafka sẽ redeliver. Sau N lần fail → vào DLT để investigate.
      // Nếu exception là business error (không thể retry), gọi ack.acknowledge() để
      // skip.
    }
  }

  @KafkaListener(
      topics = KafkaTopics.IDENTITY_ACCOUNT_STATUS_CHANGED,
      groupId = "organization-service",
      containerFactory = "kafkaListenerContainerFactory")
  public void onAccountStatusChanged(ConsumerRecord<String, Object> record, Acknowledgment ack) {
    try {
      AccountStatusChangedEvent event =
          kafkaObjectMapper.convertValue(record.value(), AccountStatusChangedEvent.class);

      log.info(
          "[Kafka] Received AccountStatusChangedEvent accountId={} {} → {}",
          event.accountId(),
          event.previousStatus(),
          event.newStatus());

      profileProvisioningService.syncProfileStatus(event);

      ack.acknowledge();

    } catch (Exception ex) {
      log.error("[Kafka] Failed to process AccountStatusChangedEvent key={}", record.key(), ex);
    }
  }
}
