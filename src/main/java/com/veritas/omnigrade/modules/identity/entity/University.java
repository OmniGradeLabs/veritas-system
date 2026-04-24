package com.veritas.omnigrade.modules.identity.entity;

import com.veritas.omnigrade.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "university")
@Getter
@Setter
public class University extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 100)
  private String domain;
}
