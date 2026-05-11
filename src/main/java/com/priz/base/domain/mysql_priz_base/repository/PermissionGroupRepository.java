package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.PermissionGroupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionGroupRepository extends JpaRepository<PermissionGroupModel, String>,
        JpaSpecificationExecutor<PermissionGroupModel> {

    Optional<PermissionGroupModel> findByCode(String code);

    boolean existsByCode(String code);
}
