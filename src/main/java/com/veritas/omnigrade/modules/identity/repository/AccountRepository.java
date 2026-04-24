package com.veritas.omnigrade.modules.identity.repository;

import com.veritas.omnigrade.modules.identity.dto.auth.projection.AccountCoreAuthProjection;
import com.veritas.omnigrade.modules.identity.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  @Query(
      """
                SELECT a.university.id AS universityId, a.role AS role
                FROM Account a
                WHERE a.id = :accountId AND a.deletedAt IS NULL
            """)
  Optional<AccountCoreAuthProjection> findCoreAuthInfoById(@Param("accountId") UUID accountId);

  Optional<Account> findByUsernameAndUniversityId(String username, UUID universityId);

  boolean existsByUsernameAndUniversityId(String username, UUID universityId);
}
