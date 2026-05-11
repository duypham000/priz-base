package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupCredentialsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ApiKeyPermissionGroupCredentialsRepository
        extends JpaRepository<ApiKeyPermissionGroupCredentialsModel, String>,
        JpaSpecificationExecutor<ApiKeyPermissionGroupCredentialsModel> {

    List<ApiKeyPermissionGroupCredentialsModel> findByApiKeyCredentialsId(String apiKeyCredentialsId);

    @Query("SELECT m.permissionGroupCode FROM ApiKeyPermissionGroupCredentialsModel m "
            + "WHERE m.apiKeyCredentialsId = :credentialsId")
    List<String> findGroupCodesByApiKeyCredentialsId(@Param("credentialsId") String credentialsId);

    boolean existsByApiKeyCredentialsIdAndPermissionGroupCode(
            String apiKeyCredentialsId, String permissionGroupCode);

    @Modifying
    @Transactional
    long deleteByApiKeyCredentialsIdAndPermissionGroupCode(
            String apiKeyCredentialsId, String permissionGroupCode);

    @Modifying
    @Transactional
    long deleteByApiKeyCredentialsId(String apiKeyCredentialsId);
}
