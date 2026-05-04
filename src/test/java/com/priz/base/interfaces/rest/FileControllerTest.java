package com.priz.base.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.files.dto.FileSyncRequest;
import com.priz.interfaces.admin.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FileService fileService;

    @Test
    void upload_should_return201_withFileDetail() throws Exception {
        FileDetailResponse detail = FileDetailResponse.builder()
                .id("file-1")
                .originalName("test.txt")
                .fileType("txt")
                .fileSize(1024L)
                .contentType("text/plain")
                .isSynced(false)
                .createdAt(Instant.now())
                .build();

        when(fileService.upload(any(), any())).thenReturn(detail);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "file content".getBytes());

        mockMvc.perform(multipart("/api/files").file(file)
                        .param("description", "Test file"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.originalName").value("test.txt"));
    }

    @Test
    void download_should_return200_withOctetStream() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("file content".getBytes());
        when(fileService.download("file-1")).thenReturn(resource);
        when(fileService.getOriginalFilename("file-1")).thenReturn("test.txt");

        mockMvc.perform(get("/api/files/file-1/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    void getFileDetail_should_return200() throws Exception {
        FileDetailResponse detail = FileDetailResponse.builder()
                .id("file-1")
                .originalName("test.txt")
                .build();

        when(fileService.getFileDetail("file-1")).thenReturn(detail);

        mockMvc.perform(get("/api/files/file-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("file-1"));
    }

    @Test
    void getFileList_should_return200_withPageResponse() throws Exception {
        PageResponse<FileDetailResponse> pageResponse = PageResponse.<FileDetailResponse>builder()
                .content(List.of())
                .page(1) // Changed from 0 to 1
                .pageSize(20)
                .total(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(fileService.getFileList(any())).thenReturn(pageResponse);

        FileFilterRequest filter = new FileFilterRequest();

        mockMvc.perform(post("/api/files/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void deleteFile_should_return200() throws Exception {
        mockMvc.perform(delete("/api/files/file-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File deleted successfully"));
    }

    @Test
    void syncFiles_should_return200() throws Exception {
        FileSyncRequest request = FileSyncRequest.builder()
                .files(List.of(
                        FileSyncRequest.FileSyncItem.builder()
                                .sourceUrl("https://example.com/file.txt")
                                .originalName("file.txt")
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/files/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Files synced successfully"));
    }
}
