package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.converter.FileConverter;
import com.priz.base.application.features.files.dto.*;
import com.priz.base.application.features.files.event.FileProcessEvent;
import com.priz.base.application.features.files.helper.FileSpecification;
import com.priz.base.domain.mysql_priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql_priz_base.repository.FileUploadJobRepository;
import com.priz.base.infrastructure.kafka.producer.FileProcessEventProducer;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import com.priz.base.domain.mysql_priz_base.repository.FileRepository;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FileUploadJobRepository jobRepository;
    private final LocalStorageService storageService;
    private final FileProcessEventProducer eventProducer;

    @Override
    @Transactional
    public AsyncUploadResponse upload(MultipartFile file, String description) {
        String userId = SecurityContextHolder.getCurrentUserId();

        String storedName = storageService.store(file);

        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);

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

        FileUploadJobModel job = FileUploadJobModel.builder()
                .fileId(fileModel.getId())
                .userId(userId)
                .status(FileUploadJobModel.Status.PENDING)
                .build();
        job = jobRepository.save(job);

        String fileContent = null;
        if (isIndexable(file.getContentType(), extension)) {
            fileContent = readFileContent(file);
        }

        FileProcessEvent event = FileProcessEvent.builder()
                .jobId(job.getId())
                .fileId(fileModel.getId())
                .userId(userId)
                .storedName(storedName)
                .originalName(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileContent(fileContent)
                .operation(FileProcessEvent.OperationType.UPLOAD)
                .build();

        eventProducer.publish(event);

        log.info("Initiated file upload jobId={} fileId={} user={}", job.getId(), fileModel.getId(), userId);

        return AsyncUploadResponse.builder()
                .jobId(job.getId())
                .fileId(fileModel.getId())
                .status(job.getStatus().name())
                .message("File queued for processing")
                .createdAt(job.getCreatedAt())
                .build();
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

        FileProcessEvent event = FileProcessEvent.builder()
                .fileId(fileId)
                .userId(userId)
                .originalName(fileModel.getOriginalName())
                .operation(FileProcessEvent.OperationType.DELETE)
                .build();
        eventProducer.publish(event);

        log.info("File deleted and delete event published: {} by user: {}", fileId, userId);
    }

    @Override
    @Transactional
    public List<AsyncUploadResponse> syncFiles(FileSyncRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        List<AsyncUploadResponse> responses = new java.util.ArrayList<>();

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

            fileModel = fileRepository.save(fileModel);

            FileUploadJobModel job = FileUploadJobModel.builder()
                    .fileId(fileModel.getId())
                    .userId(userId)
                    .status(FileUploadJobModel.Status.PENDING)
                    .build();
            job = jobRepository.save(job);

            FileProcessEvent event = FileProcessEvent.builder()
                    .jobId(job.getId())
                    .fileId(fileModel.getId())
                    .userId(userId)
                    .originalName(item.getOriginalName())
                    .contentType(item.getContentType())
                    .fileSize(item.getFileSize())
                    .sourceUrl(item.getSourceUrl())
                    .operation(FileProcessEvent.OperationType.SYNC)
                    .build();

            eventProducer.publish(event);

            responses.add(AsyncUploadResponse.builder()
                    .jobId(job.getId())
                    .fileId(fileModel.getId())
                    .status(job.getStatus().name())
                    .message("File sync queued for processing")
                    .createdAt(job.getCreatedAt())
                    .build());
        }

        log.info("Queued sync for {} files for user: {}", request.getFiles().size(), userId);
        return responses;
    }

    @Override
    @Transactional
    public void updateFile(String fileId, UpdateFileRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);

        if (request.getDescription() != null) {
            fileModel.setDescription(request.getDescription());
        }
        fileRepository.save(fileModel);

        FileProcessEvent event = FileProcessEvent.builder()
                .fileId(fileId)
                .userId(userId)
                .originalName(fileModel.getOriginalName())
                .description(request.getDescription())
                .fileContent(request.getContent())
                .operation(FileProcessEvent.OperationType.UPDATE)
                .build();
        eventProducer.publish(event);

        log.info("File updated and update event published: {} by user: {}", fileId, userId);
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }

    private boolean isIndexable(String contentType, String extension) {
        return "txt".equalsIgnoreCase(extension) || "md".equalsIgnoreCase(extension)
                || "text/plain".equalsIgnoreCase(contentType) || "text/markdown".equalsIgnoreCase(contentType);
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            log.warn("Failed to read file content for indexing: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(String fileId) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel fileModel = findFileByIdAndUser(fileId, userId);
        return fileModel.getOriginalName();
    }

    @Override
    @Transactional(readOnly = true)
    public UploadJobStatusResponse getJobStatus(String jobId) {
        FileUploadJobModel job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("UploadJob", "id", jobId));

        return UploadJobStatusResponse.builder()
                .jobId(job.getId())
                .fileId(job.getFileId())
                .userId(job.getUserId())
                .status(job.getStatus().name())
                .telegramUrl(job.getTelegramUrl())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .kafkaPartition(job.getKafkaPartition())
                .kafkaOffset(job.getKafkaOffset())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
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
