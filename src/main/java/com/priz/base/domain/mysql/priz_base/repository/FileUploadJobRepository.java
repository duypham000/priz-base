package com.priz.base.domain.mysql.priz_base.repository;

import com.priz.base.domain.mysql.priz_base.model.FileUploadJobModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FileUploadJobRepository extends JpaRepository<FileUploadJobModel, String>,
        JpaSpecificationExecutor<FileUploadJobModel> {

    Optional<FileUploadJobModel> findByFileId(String fileId);

    List<FileUploadJobModel> findByStatus(FileUploadJobModel.Status status);

    List<FileUploadJobModel> findByUserId(String userId);
}
