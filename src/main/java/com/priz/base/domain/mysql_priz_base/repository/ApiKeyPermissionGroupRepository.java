package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyPermissionGroupRepository
        extends JpaRepository<ApiKeyPermissionGroupModel, String>,
        JpaSpecificationExecutor<ApiKeyPermissionGroupModel> {

    Optional<ApiKeyPermissionGroupModel> findByCode(String code);

    boolean existsByCode(String code);
}
