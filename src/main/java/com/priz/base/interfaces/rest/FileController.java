package com.priz.base.interfaces.rest;

import com.priz.base.application.features.files.FileService;
import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.files.dto.FileSyncRequest;
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
@Secured
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file")
    public ResponseEntity<ApiResponse<FileDetailResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        FileDetailResponse response = fileService.upload(file, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

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

    @GetMapping("/{id}")
    @Operation(summary = "Get file detail")
    public ResponseEntity<ApiResponse<FileDetailResponse>> getFileDetail(
            @PathVariable String id) {
        FileDetailResponse response = fileService.getFileDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @Operation(summary = "Get file list with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<FileDetailResponse>>> getFileList(
            @Valid @RequestBody FileFilterRequest filter) {
        PageResponse<FileDetailResponse> response = fileService.getFileList(filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync files detail from source to database")
    public ResponseEntity<ApiResponse<Void>> syncFiles(
            @Valid @RequestBody FileSyncRequest request) {
        fileService.syncFiles(request);
        return ResponseEntity.ok(ApiResponse.success("Files synced successfully", null));
    }
}
