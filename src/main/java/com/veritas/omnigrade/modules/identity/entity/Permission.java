package com.veritas.omnigrade.modules.identity.entity;

import com.veritas.omnigrade.infrastructure.persistence.BaseEntity;
import com.veritas.omnigrade.modules.identity.enumType.PermissionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "permission")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Permission extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, length = 50)
  private String scope;

  @Column(length = 80)
  private String module;

  @Column(length = 120, nullable = false)
  private String resource;

  @Column(length = 160, nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PermissionAction action;
}
