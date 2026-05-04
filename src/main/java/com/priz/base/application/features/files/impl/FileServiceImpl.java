package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.converter.FileConverter;
import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.files.dto.FileSyncRequest;
import com.priz.base.application.features.files.helper.FileSpecification;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql.priz_base.model.FileModel;
import com.priz.base.domain.mysql.priz_base.repository.FileRepository;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final LocalStorageService storageService;

    @Override
    @Transactional
    public FileDetailResponse upload(MultipartFile file, String description) {
        String userId = SecurityContextHolder.getCurrentUserId();

        String storedName = storageService.store(file);

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".") + 1);
        }

        FileModel fileModel = FileModel.builder()
                .originalName(originalName != null ? originalName : "unknown")
                .storedName(storedName)
                .filePath(storageService.getRootLocation().resolve(storedName).toString())
                .fileType(extension)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .userId(userId)
                .description(description)
                .isSynced(false)
                .build();

        fileModel = fileRepository.save(fileModel);
        log.info("File uploaded: {} by user: {}", originalName, userId);

        return FileConverter.toDetailResponse(fileModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(String fileId) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);
        return storageService.loadAsResource(fileModel.getStoredName());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDetailResponse getFileDetail(String fileId) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);
        return FileConverter.toDetailResponse(fileModel);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileDetailResponse> getFileList(FileFilterRequest filter) {
        String userId = SecurityContextHolder.getCurrentUserId();

        Specification<FileModel> spec = FileSpecification.withFilters(userId, filter);

        Page<FileModel> page = fileRepository.findAll(spec,
                filter.getPagination().toPageable());

        Page<FileDetailResponse> responsePage = page.map(FileConverter::toDetailResponse);
        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);

        storageService.delete(fileModel.getStoredName());

        fileRepository.delete(fileModel);
        log.info("File deleted: {} by user: {}", fileId, userId);
    }

    @Override
    @Transactional
    public void syncFiles(FileSyncRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();

        for (FileSyncRequest.FileSyncItem item : request.getFiles()) {
            FileModel fileModel = FileModel.builder()
                    .originalName(item.getOriginalName())
                    .storedName("synced-" + UUID.randomUUID())
                    .filePath(item.getSourceUrl())
                    .fileType(item.getFileType())
                    .fileSize(item.getFileSize())
                    .contentType(item.getContentType())
                    .userId(userId)
                    .description(item.getDescription())
                    .sourceUrl(item.getSourceUrl())
                    .isSynced(true)
                    .build();

            fileRepository.save(fileModel);
        }

        log.info("Synced {} files for user: {}", request.getFiles().size(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(String fileId) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);
        return fileModel.getOriginalName();
    }

    private FileModel findFileByIdAndUser(String fileId, String userId) {
        FileModel fileModel = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        if (!fileModel.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this file");
        }

        return fileModel;
    }
}
