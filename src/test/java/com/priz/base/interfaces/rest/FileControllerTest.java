package com.priz.base.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.features.files.FileSearchService;
import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.dto.*;
import com.priz.common.security.jwt.JwtService;
import com.priz.interfaces.admin.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private FileSearchService fileSearchService;

    @Test
    void upload_should_return202_withAsyncResponse() throws Exception {
        AsyncUploadResponse response = AsyncUploadResponse.builder()
                .jobId("job-1")
                .fileId("file-1")
                .build();

        when(fileService.upload(any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "file content".getBytes());

        mockMvc.perform(multipart("/api/files").file(file)
                        .param("description", "Test file"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.data.jobId").value("job-1"));
    }

    @Test
    void download_should_return200_withOctetStream() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("file content".getBytes());
        when(fileService.download("file-1")).thenReturn(resource);
        when(fileService.getOriginalFilename("file-1")).thenReturn("test.txt");

        mockMvc.perform(get("/api/files/file-1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    void deleteFile_should_return200() throws Exception {
        mockMvc.perform(delete("/api/files/file-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File deletion initiated"));
    }

    @Test
    void syncFiles_should_return202() throws Exception {
        FileSyncRequest request = FileSyncRequest.builder()
                .files(List.of(
                        FileSyncRequest.FileSyncItem.builder()
                                .sourceUrl("https://example.com/file.txt")
                                .originalName("file.txt")
                                .build()
                ))
                .build();

        when(fileService.syncFiles(any())).thenReturn(List.of(AsyncUploadResponse.builder().jobId("job-1").build()));

        mockMvc.perform(post("/api/files/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void search_should_return200() throws Exception {
        PageResponse<FileDetailResponse> pageResponse = PageResponse.<FileDetailResponse>builder()
                .content(List.of())
                .total(0)
                .build();

        when(fileSearchService.search(any())).thenReturn(pageResponse);

        FileSearchRequest request = new FileSearchRequest();
        request.setQuery("test");

        mockMvc.perform(post("/api/files/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
