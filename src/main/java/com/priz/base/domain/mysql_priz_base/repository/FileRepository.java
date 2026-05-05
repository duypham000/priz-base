package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.FileModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileModel, String>,
        JpaSpecificationExecutor<FileModel> {

    List<FileModel> findByUserId(String userId);

    Optional<FileModel> findByIdAndUserId(String id, String userId);

    Optional<FileModel> findByStoredName(String storedName);

    List<FileModel> findByUserIdAndIsSyncedFalse(String userId);
}
