package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.AsyncFileUploadService;
import com.priz.base.application.features.files.dto.AsyncUploadResponse;
import com.priz.base.application.features.files.dto.UploadJobStatusResponse;
import com.priz.base.application.features.files.event.FileUploadEvent;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql.priz_base.model.FileModel;
import com.priz.base.domain.mysql.priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql.priz_base.repository.FileRepository;
import com.priz.base.domain.mysql.priz_base.repository.FileUploadJobRepository;
import com.priz.base.infrastructure.kafka.producer.FileUploadEventProducer;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileUploadServiceImpl implements AsyncFileUploadService {

    private final FileRepository fileRepository;
    private final FileUploadJobRepository jobRepository;
    private final LocalStorageService storageService;
    private final FileUploadEventProducer eventProducer;

    @Override
    @Transactional
    public AsyncUploadResponse initiateUpload(MultipartFile file, String description) {
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

        FileUploadJobModel job = FileUploadJobModel.builder()
                .fileId(fileModel.getId())
                .userId(userId)
                .status(FileUploadJobModel.Status.PENDING)
                .build();
        job = jobRepository.save(job);

        FileUploadEvent event = FileUploadEvent.builder()
                .jobId(job.getId())
                .fileId(fileModel.getId())
                .userId(userId)
                .storedName(storedName)
                .originalName(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        eventProducer.publish(event);

        log.info("Initiated async upload jobId={} fileId={} user={}", job.getId(), fileModel.getId(), userId);

        return AsyncUploadResponse.builder()
                .jobId(job.getId())
                .fileId(fileModel.getId())
                .status(job.getStatus().name())
                .message("File queued for upload to Telegram")
                .createdAt(job.getCreatedAt())
                .build();
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
}
