package com.veritas.omnigrade.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforce architectural boundaries giữa các module bằng ArchUnit.
 *
 * <p>Test này chạy cùng {@code mvn test} — sẽ fail build (và CI) ngay khi ai vi phạm rule.
 *
 * <h2>Rules được enforce</h2>
 *
 * <ol>
 *   <li>identity KHÔNG được import bất cứ thứ gì từ organization
 *   <li>organization KHÔNG được import service/entity/repository của identity (chỉ được import
 *       event DTOs)
 *   <li>Các module KHÔNG được inject KafkaTemplate trực tiếp — phải qua DomainEventPublisher
 *   <li>Consumer (@KafkaListener) chỉ được đặt trong package {@code event.listener}
 *   <li>infrastructure KHÔNG được import từ modules (dependency chiều ngược)
 * </ol>
 */
class ModuleDependencyRulesTest {

  private static final String ROOT = "com.veritas.omnigrade";
  private static final String IDENTITY = ROOT + ".modules.identity";
  private static final String ORGANIZATION = ROOT + ".modules.organization";
  private static final String INFRASTRUCTURE = ROOT + ".infrastructure";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    // Import tất cả class trong project, bỏ qua các class test
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  // ── Rule 1: identity không được biết gì về organization ──────────────────────

  @Test
  @DisplayName("identity module KHÔNG được import bất cứ thứ gì từ organization module")
  void identity_must_not_depend_on_organization() {
    noClasses()
        .that()
        .resideInAPackage(IDENTITY + "..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ORGANIZATION + "..")
        .because(
            "identity là tầng 1 — giao tiếp với tầng 2 (organization) phải qua Kafka events, KHÔNG phải direct import")
        .check(classes);
  }

  // ── Rule 2: organization chỉ được import event DTOs từ identity ──────────────

  @Test
  @DisplayName(
      "organization chỉ được import package 'event' của identity, không import service/entity/repository")
  void organization_may_only_import_identity_events() {
    noClasses()
        .that()
        .resideInAPackage(ORGANIZATION + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            IDENTITY + ".service..",
            IDENTITY + ".entity..",
            IDENTITY + ".repository..",
            IDENTITY + ".controller..",
            IDENTITY + ".mapper..",
            IDENTITY + ".dto..")
        .because(
            "organization không được gọi trực tiếp vào service/entity/repository của identity. "
                + "Chỉ được import từ identity.event (event DTO) để deserialize Kafka message")
        .check(classes);
  }

  // ── Rule 3: modules không được inject KafkaTemplate trực tiếp ────────────────

  @Test
  @DisplayName("Modules KHÔNG được inject KafkaTemplate trực tiếp — phải dùng DomainEventPublisher")
  void modules_must_not_use_kafka_template_directly() {
    noClasses()
        .that()
        .resideInAnyPackage(IDENTITY + "..", ORGANIZATION + "..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.springframework.kafka.core.KafkaTemplate")
        .because(
            "Modules chỉ được dùng DomainEventPublisher (infrastructure.messaging.kafka). "
                + "Không inject KafkaTemplate trực tiếp để tránh bypass logging, error handling")
        .check(classes);
  }

  // ── Rule 4: @KafkaListener chỉ được đặt trong package event.listener ─────────

  @Test
  @DisplayName("@KafkaListener chỉ được đặt trong package 'event.listener' của mỗi module")
  void kafka_listeners_must_reside_in_event_listener_package() {
    classes()
        .that()
        .areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
        .or()
        .haveSimpleNameEndingWith("EventListener")
        .should()
        .resideInAPackage("..event.listener..")
        .because(
            "Đặt consumer ngoài package event.listener khiến boundary không rõ ràng và khó tìm khi debug")
        .check(classes);
  }

  // ── Rule 5: infrastructure không được import từ modules ──────────────────────

  @Test
  @DisplayName("infrastructure layer KHÔNG được import từ modules (dependency chiều ngược)")
  void infrastructure_must_not_depend_on_modules() {
    noClasses()
        .that()
        .resideInAPackage(INFRASTRUCTURE + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(IDENTITY + "..", ORGANIZATION + "..")
        .because(
            "infrastructure là tầng thấp nhất — nếu nó import modules thì tạo circular dependency. "
                + "Modules import infrastructure, không phải ngược lại")
        .check(classes);
  }

  // ── Rule 6: Event DTO không được có Spring annotation ────────────────────────

  @Test
  @DisplayName("Event record trong package 'event' không được có Spring stereotype annotation")
  void event_dtos_must_not_be_spring_beans() {
    noClasses()
        .that()
        .resideInAPackage("..modules..event")
        .and()
        .areNotInterfaces()
        // chỉ check class (record là class)
        .should()
        .beAnnotatedWith("org.springframework.stereotype.Component")
        .orShould()
        .beAnnotatedWith("org.springframework.stereotype.Service")
        .orShould()
        .beAnnotatedWith("org.springframework.stereotype.Repository")
        .because("Event DTO phải là plain Java record — không phải Spring bean")
        .check(classes);
  }
}
