package com.veritas.omnigrade.modules.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "examiner")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Examiner {

  @Id
  @Column(name = "account_id")
  private UUID accountId;

  @Column(name = "examiner_code", nullable = false, length = 20)
  private String examinerCode;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Column(length = 150)
  private String department;

  @Column(name = "avatar_url", columnDefinition = "TEXT")
  private String avatarUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
