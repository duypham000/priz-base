package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionMappingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ApiKeyPermissionMappingRepository
        extends JpaRepository<ApiKeyPermissionMappingModel, String>,
        JpaSpecificationExecutor<ApiKeyPermissionMappingModel> {

    List<ApiKeyPermissionMappingModel> findByApiKeyCredentialsId(String apiKeyCredentialsId);

    @Query("SELECT m.permissionCode FROM ApiKeyPermissionMappingModel m "
            + "WHERE m.apiKeyCredentialsId = :credentialsId")
    List<String> findPermissionCodesByApiKeyCredentialsId(@Param("credentialsId") String credentialsId);

    boolean existsByApiKeyCredentialsIdAndPermissionCode(
            String apiKeyCredentialsId, String permissionCode);

    @Modifying
    @Transactional
    long deleteByApiKeyCredentialsIdAndPermissionCode(
            String apiKeyCredentialsId, String permissionCode);

    @Modifying
    @Transactional
    long deleteByApiKeyCredentialsId(String apiKeyCredentialsId);
}
