package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyPermissionRepository extends JpaRepository<ApiKeyPermissionModel, String>,
        JpaSpecificationExecutor<ApiKeyPermissionModel> {

    Optional<ApiKeyPermissionModel> findByCode(String code);

    boolean existsByCode(String code);
}
