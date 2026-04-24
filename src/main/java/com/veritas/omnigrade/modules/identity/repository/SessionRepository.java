package com.veritas.omnigrade.modules.identity.repository;

import com.veritas.omnigrade.modules.identity.entity.Session;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessionRepository extends JpaRepository<Session, UUID> {
  // update new jti for session, return 1 if sucess, 0 if fail
  @Modifying
  @Transactional
  @Query(
      """
                update Session s
                   set s.refreshJti = :newJti,
                       s.updatedAt = CURRENT_TIMESTAMP
                 where s.id = :sessionId
                   and s.refreshJti = :oldJti
                   and s.revokedAt is null
                   and s.expiresAt > CURRENT_TIMESTAMP
            """)
  int rotateRefreshJti(
      @Param("sessionId") UUID sessionId,
      @Param("oldJti") UUID oldJti,
      @Param("newJti") UUID newJti);

  // check any session is active, use by gaurd if redis down
  @Query(
      """
                select (count(s) > 0)
                from Session s
                where s.id = :sessionId
                  and s.revokedAt is null
                  and s.expiresAt > :now
            """)
  boolean isSessionActive(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);

  // revoke session if session.revoke_at = null
  @Modifying
  @Query(
      """
                update Session s
                set s.revokedAt = :now
                where s.id = :sessionId
                  and s.revokedAt is null
            """)
  int revokeIfNotRevoked(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);

  @Modifying
  @Query(
      """
                update Session s
                set s.revokedAt = :now
                where s.user.id = :userId
                  and s.revokedAt is null
                  and s.expiresAt > :now
            """)
  int revokeAllActiveByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

  @Query(
      """
                select s.id
                from Session s
                where s.user.id = :userId
                  and s.revokedAt is null
                  and s.expiresAt > :now
            """)
  List<UUID> findActiveSessionIdsByUserId(
      @Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
