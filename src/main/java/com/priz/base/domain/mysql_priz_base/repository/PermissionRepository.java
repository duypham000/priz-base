package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionModel, String>,
        JpaSpecificationExecutor<PermissionModel> {

    Optional<PermissionModel> findByCode(String code);

    boolean existsByCode(String code);

    List<PermissionModel> findByCodeIn(List<String> codes);
}
