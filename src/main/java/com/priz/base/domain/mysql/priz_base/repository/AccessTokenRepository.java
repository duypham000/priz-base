package com.priz.base.domain.mysql.priz_base.repository;

import com.priz.base.domain.mysql.priz_base.model.AccessTokenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessTokenModel, String>,
        JpaSpecificationExecutor<AccessTokenModel> {

    Optional<AccessTokenModel> findByToken(String token);

    @Modifying
    @Query("UPDATE AccessTokenModel t SET t.isRevoked = true WHERE t.userId = :userId")
    void revokeAllByUserId(String userId);

    @Modifying
    @Query("DELETE FROM AccessTokenModel t WHERE t.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
