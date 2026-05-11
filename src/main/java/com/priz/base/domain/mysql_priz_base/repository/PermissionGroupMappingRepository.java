package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;
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
public interface PermissionGroupMappingRepository
        extends JpaRepository<PermissionGroupMappingModel, String>,
        JpaSpecificationExecutor<PermissionGroupMappingModel> {

    List<PermissionGroupMappingModel> findByPermissionGroupCode(String permissionGroupCode);

    @Query("SELECT DISTINCT m.permissionCode FROM PermissionGroupMappingModel m "
            + "WHERE m.permissionGroupCode IN :groupCodes")
    List<String> findDistinctPermissionCodesByGroupCodes(
            @Param("groupCodes") Collection<String> groupCodes);

    /**
     * Trả về list [customKey, actionMask] cho các group codes.
     * Dùng để encode bitmask permissions vào JWT claim.
     */
    @Query("SELECT p.customKey, p.actionMask FROM PermissionGroupMappingModel m "
            + "JOIN PermissionModel p ON p.code = m.permissionCode "
            + "WHERE m.permissionGroupCode IN :groupCodes "
            + "AND p.customKey IS NOT NULL AND p.actionMask > 0")
    List<Object[]> findCustomKeyMasksByGroupCodes(
            @Param("groupCodes") Collection<String> groupCodes);

    boolean existsByPermissionGroupCodeAndPermissionCode(
            String permissionGroupCode, String permissionCode);

    @Modifying
    @Transactional
    long deleteByPermissionGroupCodeAndPermissionCode(
            String permissionGroupCode, String permissionCode);

    @Modifying
    @Transactional
    long deleteByPermissionGroupCode(String permissionGroupCode);
}
