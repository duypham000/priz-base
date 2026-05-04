package com.priz.base.application.features.files;

import com.priz.base.application.features.files.dto.AsyncUploadResponse;
import com.priz.base.application.features.files.dto.UploadJobStatusResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AsyncFileUploadService {

    AsyncUploadResponse initiateUpload(MultipartFile file, String description);

    UploadJobStatusResponse getJobStatus(String jobId);
}
