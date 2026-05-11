package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserPermissionGroupRepository
        extends JpaRepository<UserPermissionGroupModel, String>,
        JpaSpecificationExecutor<UserPermissionGroupModel> {

    List<UserPermissionGroupModel> findByUserId(String userId);

    @Query("SELECT u.permissionGroupCode FROM UserPermissionGroupModel u WHERE u.userId = :userId")
    List<String> findGroupCodesByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndPermissionGroupCode(String userId, String permissionGroupCode);

    @Modifying
    @Transactional
    long deleteByUserIdAndPermissionGroupCode(String userId, String permissionGroupCode);

    @Modifying
    @Transactional
    long deleteByUserId(String userId);
}
