package com.veritas.omnigrade.infrastructure.messaging.kafka;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper quanh KafkaTemplate — dùng cho tất cả module publish event.
 *
 * <p>Modules KHÔNG inject KafkaTemplate trực tiếp. Luôn dùng DomainEventPublisher.
 *
 * <pre>
 * // Trong IdentityServiceImpl:
 * domainEventPublisher.publish(KafkaTopics.IDENTITY_ACCOUNT_CREATED, accountId, event);
 * </pre>
 */
@Component
public class DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Publish event với key là entityId (UUID string). Dùng key để Kafka đảm bảo ordering trong cùng
   * partition cho cùng entity.
   *
   * @param topic topic name từ KafkaTopics constant
   * @param entityId UUID của entity — dùng làm partition key
   * @param event event record (bất kỳ object JSON-serializable)
   */
  public void publish(String topic, String entityId, Object event) {
    CompletableFuture<SendResult<String, Object>> future =
        kafkaTemplate.send(topic, entityId, event);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error(
                "[Kafka] FAILED to publish event to topic={} key={} event={}",
                topic,
                entityId,
                event.getClass().getSimpleName(),
                ex);
          } else {
            log.debug(
                "[Kafka] Published topic={} partition={} offset={} key={}",
                topic,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                entityId);
          }
        });
  }

  /**
   * Fire-and-forget không cần key (khi entityId không quan trọng). Kafka sẽ round-robin partition.
   */
  public void publish(String topic, Object event) {
    publish(topic, null, event);
  }
}
