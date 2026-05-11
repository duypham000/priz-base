package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupMappingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApiKeyPermissionGroupMappingRepository
        extends JpaRepository<ApiKeyPermissionGroupMappingModel, String>,
        JpaSpecificationExecutor<ApiKeyPermissionGroupMappingModel> {

    List<ApiKeyPermissionGroupMappingModel> findByPermissionGroupCode(String permissionGroupCode);

    @Query("SELECT DISTINCT m.permissionCode FROM ApiKeyPermissionGroupMappingModel m "
            + "WHERE m.permissionGroupCode IN :groupCodes")
    List<String> findDistinctPermissionCodesByGroupCodes(
            @Param("groupCodes") Collection<String> groupCodes);

    boolean existsByPermissionGroupCodeAndPermissionCode(
            String permissionGroupCode, String permissionCode);

    @Modifying
    @Transactional
    long deleteByPermissionGroupCodeAndPermissionCode(
            String permissionGroupCode, String permissionCode);
}
