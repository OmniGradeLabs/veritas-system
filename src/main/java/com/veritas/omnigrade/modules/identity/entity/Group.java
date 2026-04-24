package com.veritas.omnigrade.modules.identity.entity;

import com.veritas.omnigrade.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "permission_group",
    indexes = {@Index(name = "idx_group_university", columnList = "university_id")})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Group extends BaseEntity {

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "university_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_group_university"))
  private University university;
}
