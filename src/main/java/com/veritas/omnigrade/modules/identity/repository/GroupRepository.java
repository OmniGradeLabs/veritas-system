package com.veritas.omnigrade.modules.identity.repository;

import com.veritas.omnigrade.modules.identity.entity.Group;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, UUID> {

  @Query(
      value =
          """
                        SELECT DISTINCT g.name
                        FROM account_group ag
                        JOIN user_group g ON g.id = ag.group_id
                        WHERE ag.account_id = :accountId
                    """,
      nativeQuery = true)
  List<String> findGroupNamesByAccountId(@Param("accountId") UUID accountId);
}
