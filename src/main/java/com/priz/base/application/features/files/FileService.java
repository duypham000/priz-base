package com.priz.base.application.features.files;

import com.priz.base.application.features.files.dto.*;
import com.priz.interfaces.admin.dto.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    AsyncUploadResponse upload(MultipartFile file, String description);

    Resource download(String fileId);

    FileDetailResponse getFileDetail(String fileId);

    PageResponse<FileDetailResponse> getFileList(FileFilterRequest filter);

    void deleteFile(String fileId);

    List<AsyncUploadResponse> syncFiles(FileSyncRequest request);

    void updateFile(String fileId, UpdateFileRequest request);

    UploadJobStatusResponse getJobStatus(String jobId);

    String getOriginalFilename(String fileId);
}
