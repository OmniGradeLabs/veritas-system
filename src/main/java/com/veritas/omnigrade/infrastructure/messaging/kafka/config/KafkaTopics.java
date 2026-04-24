package com.veritas.omnigrade.infrastructure.messaging.kafka.config;

/**
 * Central registry of all Kafka topic names.
 *
 * <p>Naming convention: {module}.{entity}.{event-past-tense}
 *
 * <p>Rule: - Only infrastructure layer uses this class directly. - Modules reference topics via
 * this constant, never hard-code strings.
 */
public final class KafkaTopics {

  private KafkaTopics() {}

  // ── identity module events ──────────────────────────────────────────────────
  public static final String IDENTITY_ACCOUNT_CREATED = "identity.account.created";
  public static final String IDENTITY_ACCOUNT_STATUS_CHANGED = "identity.account.status-changed";
  public static final String IDENTITY_ACCOUNT_DELETED = "identity.account.deleted";

  // ── organization module events ──────────────────────────────────────────────
  // (published by organization, consumed by higher-tier modules)
  public static final String ORGANIZATION_STUDENT_ENROLLED = "organization.student.enrolled";
  public static final String ORGANIZATION_EXAMINER_ASSIGNED = "organization.examiner.assigned";
}
