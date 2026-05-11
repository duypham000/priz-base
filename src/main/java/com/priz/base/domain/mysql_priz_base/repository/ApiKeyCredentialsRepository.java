package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyCredentialsRepository extends JpaRepository<ApiKeyCredentialsModel, String>,
        JpaSpecificationExecutor<ApiKeyCredentialsModel> {

    Optional<ApiKeyCredentialsModel> findByCode(String code);

    boolean existsByCode(String code);
}
