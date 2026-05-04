package com.priz.base.application.features.files;

import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.files.dto.FileSyncRequest;
import com.priz.interfaces.admin.dto.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileDetailResponse upload(MultipartFile file, String description);

    Resource download(String fileId);

    FileDetailResponse getFileDetail(String fileId);

    PageResponse<FileDetailResponse> getFileList(FileFilterRequest filter);

    void deleteFile(String fileId);

    void syncFiles(FileSyncRequest request);

    String getOriginalFilename(String fileId);
}
