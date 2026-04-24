package com.veritas.omnigrade.infrastructure.messaging.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Jackson ObjectMapper cho Kafka serialization. Tách riêng để không ảnh hưởng ObjectMapper của
   * Spring MVC.
   */
  @Bean(name = "kafkaObjectMapper")
  public ObjectMapper kafkaObjectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  // producer
  @Bean
  public ProducerFactory<String, Object> producerFactory(
      @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    // Idempotent producer: đảm bảo không duplicate khi retry
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

    var factory = new DefaultKafkaProducerFactory<String, Object>(props);
    factory.setValueSerializer(new JsonSerializer<>(kafkaObjectMapper));
    return factory;
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  // consumer
  @Bean
  public ConsumerFactory<String, Object> consumerFactory(
      @org.springframework.beans.factory.annotation.Qualifier("kafkaObjectMapper")
          ObjectMapper kafkaObjectMapper) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    // Trusted packages: toàn bộ project — không cần update khi thêm event mới
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.veritas.omnigrade.*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    // Offset bắt đầu từ earliest khi consumer group mới join
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // Tắt auto-commit — dùng MANUAL_IMMEDIATE ở container factory
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    var jsonDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper);
    jsonDeserializer.addTrustedPackages("com.veritas.omnigrade.*");
    jsonDeserializer.setUseTypeMapperForKey(false);

    return new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
  }

  /**
   * Container factory mặc định — dùng cho hầu hết @KafkaListener. AckMode.MANUAL_IMMEDIATE: ACK
   * ngay sau khi @KafkaListener method return không exception.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      ConsumerFactory<String, Object> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.setConcurrency(3); // 3 threads per listener
    return factory;
  }

  // ── Topic Auto-Creation (dev/staging) ────────────────────────────────────────
  // Trong production dùng Terraform/Kafka Admin. Đây là fallback cho local dev.
  @Bean
  public NewTopic topicAccountCreated() {
    return TopicBuilder.name(KafkaTopics.IDENTITY_ACCOUNT_CREATED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic topicAccountStatusChanged() {
    return TopicBuilder.name(KafkaTopics.IDENTITY_ACCOUNT_STATUS_CHANGED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic topicAccountDeleted() {
    return TopicBuilder.name(KafkaTopics.IDENTITY_ACCOUNT_DELETED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic topicStudentEnrolled() {
    return TopicBuilder.name(KafkaTopics.ORGANIZATION_STUDENT_ENROLLED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic topicExaminerAssigned() {
    return TopicBuilder.name(KafkaTopics.ORGANIZATION_EXAMINER_ASSIGNED)
        .partitions(3)
        .replicas(1)
        .build();
  }
}
