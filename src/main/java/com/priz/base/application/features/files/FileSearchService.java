package com.priz.base.application.features.files;

import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileSearchRequest;
import com.priz.interfaces.admin.dto.PageResponse;

public interface FileSearchService {
    PageResponse<FileDetailResponse> search(FileSearchRequest request);
}
