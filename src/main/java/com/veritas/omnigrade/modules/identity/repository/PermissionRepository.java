package com.veritas.omnigrade.modules.identity.repository;

import com.veritas.omnigrade.modules.identity.entity.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

  @Query(
      value =
          """
                        SELECT DISTINCT p.name
                        FROM account_group ag
                        JOIN group_permission gp ON gp.group_id = ag.group_id
                        JOIN permission p ON p.id = gp.permission_id
                        WHERE ag.account_id = :accountId
                    """,
      nativeQuery = true)
  List<String> findPermissionNamesByAccountId(@Param("accountId") UUID accountId);
}
