package com.priz.base.application.features.files.converter;

import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.domain.mysql.priz_base.model.FileModel;

public final class FileConverter {

    private FileConverter() {}

    public static FileDetailResponse toDetailResponse(FileModel model) {
        return FileDetailResponse.builder()
                .id(model.getId())
                .originalName(model.getOriginalName())
                .fileType(model.getFileType())
                .fileSize(model.getFileSize())
                .contentType(model.getContentType())
                .description(model.getDescription())
                .downloadUrl("/api/files/" + model.getId() + "/download")
                .isSynced(model.getIsSynced())
                .userId(model.getUserId())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
