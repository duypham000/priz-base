package com.priz.base.domain.mysql.priz_base.repository;

import com.priz.base.domain.mysql.priz_base.model.RefreshTokenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, String>,
        JpaSpecificationExecutor<RefreshTokenModel> {

    Optional<RefreshTokenModel> findByToken(String token);

    List<RefreshTokenModel> findByUserIdAndIsRevokedFalse(String userId);

    @Modifying
    @Query("UPDATE RefreshTokenModel r SET r.isRevoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(String userId);

    @Modifying
    @Query("DELETE FROM RefreshTokenModel r WHERE r.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
