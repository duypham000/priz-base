package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.dto.*;
import com.priz.base.application.features.files.dto.AsyncUploadResponse;
import com.priz.base.domain.mysql_priz_base.repository.FileUploadJobRepository;
import com.priz.base.infrastructure.kafka.producer.FileProcessEventProducer;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.PageRequestDto;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import com.priz.base.domain.mysql_priz_base.repository.FileRepository;
import com.priz.base.testutil.SecurityTestUtil;
import com.priz.base.testutil.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileUploadJobRepository jobRepository;

    @Mock
    private FileProcessEventProducer fileProcessEventProducer;

    @Mock
    private LocalStorageService storageService;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        SecurityTestUtil.setSecurityContext(
                TestFixtures.TEST_USER_ID,
                TestFixtures.TEST_EMAIL,
                TestFixtures.TEST_USERNAME,
                "USER"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtil.clearSecurityContext();
    }

    // =============================================
    // UPLOAD
    // =============================================

    @Test
    void upload_should_storeFileAndSaveModel() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        when(storageService.store(multipartFile)).thenReturn("stored-uuid.pdf");
        when(storageService.getRootLocation()).thenReturn(Path.of("/test-uploads"));

        FileModel savedFile = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.save(any(FileModel.class))).thenReturn(savedFile);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AsyncUploadResponse response = fileService.upload(multipartFile, "Test description");

        assertNotNull(response);
        verify(storageService).store(multipartFile);
        verify(fileRepository).save(any(FileModel.class));
        verify(fileProcessEventProducer).publish(any());
    }

    @Test
    void upload_should_handleFileWithNoExtension() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("readme");
        when(multipartFile.getSize()).thenReturn(512L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        try {
            lenient().when(multipartFile.getBytes()).thenReturn("file content".getBytes());
        } catch (java.io.IOException e) {
            // Should not happen
        }

        when(storageService.store(multipartFile)).thenReturn("stored-uuid");
        when(storageService.getRootLocation()).thenReturn(Path.of("/test-uploads"));

        FileModel savedFile = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.save(any(FileModel.class))).thenReturn(savedFile);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AsyncUploadResponse response = fileService.upload(multipartFile, null);

        assertNotNull(response);
        verify(fileProcessEventProducer).publish(any());
    }

    // =============================================
    // DOWNLOAD
    // =============================================

    @Test
    void download_should_returnResource_whenFileExists() {
        FileModel fileModel = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        Resource expectedResource = new ByteArrayResource("file content".getBytes());
        when(storageService.loadAsResource(fileModel.getStoredName())).thenReturn(expectedResource);

        Resource result = fileService.download(fileModel.getId());

        assertSame(expectedResource, result);
    }

    @Test
    void download_should_throwForbidden_whenUserMismatch() {
        FileModel fileModel = TestFixtures.createFileModel("other-user-id");
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        assertThrows(ForbiddenException.class, () -> fileService.download(fileModel.getId()));
    }

    @Test
    void download_should_throwNotFound_whenFileNotFound() {
        when(fileRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.download("nonexistent"));
    }

    // =============================================
    // GET FILE DETAIL
    // =============================================

    @Test
    void getFileDetail_should_returnDetail_whenOwner() {
        FileModel fileModel = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        FileDetailResponse response = fileService.getFileDetail(fileModel.getId());

        assertNotNull(response);
    }

    @Test
    void getFileDetail_should_throwForbidden_whenNotOwner() {
        FileModel fileModel = TestFixtures.createFileModel("other-user-id");
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        assertThrows(ForbiddenException.class, () -> fileService.getFileDetail(fileModel.getId()));
    }

    // =============================================
    // GET FILE LIST
    // =============================================

    @Test
    @SuppressWarnings("unchecked")
    void getFileList_should_returnPageResponse() {
        FileModel fileModel = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        Page<FileModel> page = new PageImpl<>(List.of(fileModel));

        when(fileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        FileFilterRequest filter = FileFilterRequest.builder()
                .pagination(new PageRequestDto())
                .build();

        PageResponse<FileDetailResponse> response = fileService.getFileList(filter);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    // =============================================
    // DELETE FILE
    // =============================================

    @Test
    void deleteFile_should_deleteStorageAndDbRecord() {
        FileModel fileModel = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        fileService.deleteFile(fileModel.getId());

        verify(storageService).delete(fileModel.getStoredName());
        verify(fileRepository).delete(fileModel);
    }

    @Test
    void deleteFile_should_throwForbidden_whenNotOwner() {
        FileModel fileModel = TestFixtures.createFileModel("other-user-id");
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        assertThrows(ForbiddenException.class, () -> fileService.deleteFile(fileModel.getId()));
    }

    // =============================================
    // SYNC FILES
    // =============================================

    @Test
    void syncFiles_should_saveAllSyncItems() {
        FileSyncRequest request = FileSyncRequest.builder()
                .files(List.of(
                        FileSyncRequest.FileSyncItem.builder()
                                .sourceUrl("https://example.com/file1.txt")
                                .originalName("file1.txt")
                                .fileType("txt")
                                .fileSize(1024L)
                                .contentType("text/plain")
                                .build(),
                        FileSyncRequest.FileSyncItem.builder()
                                .sourceUrl("https://example.com/file2.pdf")
                                .originalName("file2.pdf")
                                .fileType("pdf")
                                .fileSize(2048L)
                                .contentType("application/pdf")
                                .build()
                ))
                .build();

        when(fileRepository.save(any(FileModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<AsyncUploadResponse> responses = fileService.syncFiles(request);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(fileRepository, times(2)).save(any(FileModel.class));
        verify(fileProcessEventProducer, times(2)).publish(any());
    }

    // =============================================
    // GET ORIGINAL FILENAME
    // =============================================

    @Test
    void getOriginalFilename_should_returnName_whenOwner() {
        FileModel fileModel = TestFixtures.createFileModel(TestFixtures.TEST_USER_ID);
        when(fileRepository.findById(fileModel.getId())).thenReturn(Optional.of(fileModel));

        String filename = fileService.getOriginalFilename(fileModel.getId());

        assertEquals(fileModel.getOriginalName(), filename);
    }
}
