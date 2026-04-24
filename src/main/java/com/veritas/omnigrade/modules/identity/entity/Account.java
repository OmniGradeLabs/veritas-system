package com.veritas.omnigrade.modules.identity.entity;

import com.veritas.omnigrade.infrastructure.persistence.BaseEntity;
import com.veritas.omnigrade.modules.identity.enumType.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "account",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_account_username_univ",
          columnNames = {"university_id", "username"})
    })
@Getter
@Setter
public class Account extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "university_id", nullable = false)
  private University university;

  @Column(nullable = false, length = 100)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(length = 100)
  @Email(message = "Email không hợp lệ")
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountStatus status;

  @Column(name = "is_first_login", nullable = false)
  private boolean isFirstLogin = true;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
