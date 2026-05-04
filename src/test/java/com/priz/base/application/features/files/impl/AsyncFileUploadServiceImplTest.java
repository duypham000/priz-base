package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.dto.AsyncUploadResponse;
import com.priz.base.application.features.files.dto.UploadJobStatusResponse;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql.priz_base.model.FileModel;
import com.priz.base.domain.mysql.priz_base.model.FileUploadJobModel;
import com.priz.base.domain.mysql.priz_base.repository.FileRepository;
import com.priz.base.domain.mysql.priz_base.repository.FileUploadJobRepository;
import com.priz.base.infrastructure.kafka.producer.FileUploadEventProducer;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.security.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncFileUploadServiceImplTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileUploadJobRepository jobRepository;
    @Mock
    private LocalStorageService storageService;
    @Mock
    private FileUploadEventProducer eventProducer;
    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AsyncFileUploadServiceImpl service;

    private MockedStatic<SecurityContextHolder> securityContextMock;

    @BeforeEach
    void setUp() {
        securityContextMock = mockStatic(SecurityContextHolder.class);
        securityContextMock.when(SecurityContextHolder::getCurrentUserId).thenReturn("user-123");
    }

    @AfterEach
    void tearDown() {
        securityContextMock.close();
    }

    @Test
    void initiateUpload_ValidFile_ReturnsPendingJob() {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(storageService.store(multipartFile)).thenReturn("stored-uuid.pdf");
        when(storageService.getRootLocation()).thenReturn(Paths.get("./uploads"));

        FileModel savedFile = new FileModel();
        savedFile.setId("file-id-1");
        savedFile.setOriginalName("test.pdf");
        when(fileRepository.save(any(FileModel.class))).thenReturn(savedFile);

        FileUploadJobModel savedJob = FileUploadJobModel.builder()
                .fileId("file-id-1")
                .userId("user-123")
                .status(FileUploadJobModel.Status.PENDING)
                .build();
        savedJob.setId("job-id-1");
        savedJob.setCreatedAt(Instant.now());
        when(jobRepository.save(any(FileUploadJobModel.class))).thenReturn(savedJob);

        // Act
        AsyncUploadResponse response = service.initiateUpload(multipartFile, "test description");

        // Assert
        assertThat(response.getJobId()).isEqualTo("job-id-1");
        assertThat(response.getFileId()).isEqualTo("file-id-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(fileRepository).save(any(FileModel.class));
        verify(jobRepository).save(any(FileUploadJobModel.class));
        verify(eventProducer).publish(any());
    }

    @Test
    void initiateUpload_PublishesEventWithCorrectData() {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(storageService.store(multipartFile)).thenReturn("uuid.png");
        when(storageService.getRootLocation()).thenReturn(Paths.get("./uploads"));

        FileModel savedFile = new FileModel();
        savedFile.setId("file-id-2");
        when(fileRepository.save(any(FileModel.class))).thenReturn(savedFile);

        FileUploadJobModel savedJob = FileUploadJobModel.builder().build();
        savedJob.setId("job-id-2");
        savedJob.setCreatedAt(Instant.now());
        when(jobRepository.save(any(FileUploadJobModel.class))).thenReturn(savedJob);

        ArgumentCaptor<com.priz.base.application.features.files.event.FileUploadEvent> eventCaptor =
                ArgumentCaptor.forClass(com.priz.base.application.features.files.event.FileUploadEvent.class);

        // Act
        service.initiateUpload(multipartFile, null);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        com.priz.base.application.features.files.event.FileUploadEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getFileId()).isEqualTo("file-id-2");
        assertThat(capturedEvent.getUserId()).isEqualTo("user-123");
        assertThat(capturedEvent.getStoredName()).isEqualTo("uuid.png");
        assertThat(capturedEvent.getContentType()).isEqualTo("image/png");
    }

    @Test
    void getJobStatus_ExistingJob_ReturnsStatus() {
        // Arrange
        FileUploadJobModel job = FileUploadJobModel.builder()
                .fileId("file-id-3")
                .userId("user-123")
                .status(FileUploadJobModel.Status.COMPLETED)
                .telegramUrl("https://t.me/file")
                .retryCount(0)
                .build();
        job.setId("job-id-3");
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        when(jobRepository.findById("job-id-3")).thenReturn(Optional.of(job));

        // Act
        UploadJobStatusResponse response = service.getJobStatus("job-id-3");

        // Assert
        assertThat(response.getJobId()).isEqualTo("job-id-3");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getTelegramUrl()).isEqualTo("https://t.me/file");
    }

    @Test
    void getJobStatus_NonExistingJob_ThrowsResourceNotFoundException() {
        // Arrange
        when(jobRepository.findById("missing-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getJobStatus("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(fileRepository, never()).findById(any());
    }
}
