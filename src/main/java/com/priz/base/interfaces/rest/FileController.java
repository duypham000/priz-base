package com.priz.base.interfaces.rest;

import com.priz.base.application.features.files.FileSearchService;
import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.dto.*;
import com.priz.base.common.response.ApiResponse;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.common.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "File management endpoints")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileSearchService fileSearchService;

    @Secured
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file (Async)")
    public ResponseEntity<ApiResponse<AsyncUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        AsyncUploadResponse response = fileService.upload(file, description);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(response));
    }

    @Secured
    @GetMapping("/{id}/download")
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        Resource resource = fileService.download(id);
        String filename = fileService.getOriginalFilename(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Secured
    @GetMapping("/{id}")
    @Operation(summary = "Get file detail")
    public ResponseEntity<ApiResponse<FileDetailResponse>> getFileDetail(
            @PathVariable String id) {
        FileDetailResponse response = fileService.getFileDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Secured
    @PostMapping("/filter")
    @Operation(summary = "Get file list with pagination and filtering (MySQL)")
    public ResponseEntity<ApiResponse<PageResponse<FileDetailResponse>>> getFileList(
            @Valid @RequestBody FileFilterRequest filter) {
        PageResponse<FileDetailResponse> response = fileService.getFileList(filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Secured
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success("File deletion initiated", null));
    }

    @Secured
    @PostMapping("/sync")
    @Operation(summary = "Sync files from remote source (Async)")
    public ResponseEntity<ApiResponse<java.util.List<AsyncUploadResponse>>> syncFiles(
            @Valid @RequestBody FileSyncRequest request) {
        java.util.List<AsyncUploadResponse> response = fileService.syncFiles(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(response));
    }

    @Secured
    @PutMapping("/{id}")
    @Operation(summary = "Update file metadata and content")
    public ResponseEntity<ApiResponse<Void>> updateFile(
            @PathVariable String id,
            @Valid @RequestBody UpdateFileRequest request) {
        fileService.updateFile(id, request);
        return ResponseEntity.ok(ApiResponse.success("File update initiated", null));
    }

    @Secured
    @PostMapping("/search")
    @Operation(summary = "Search files using Elasticsearch (txt/md content)")
    public ResponseEntity<ApiResponse<PageResponse<FileDetailResponse>>> search(
            @Valid @RequestBody FileSearchRequest request) {
        PageResponse<FileDetailResponse> response = fileSearchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Secured
    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Poll async upload job status")
    public ResponseEntity<ApiResponse<UploadJobStatusResponse>> getJobStatus(
            @PathVariable String jobId) {
        UploadJobStatusResponse response = fileService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
