package com.priz.base.application.features.files.impl;

import com.priz.base.application.features.files.FileSearchService;
import com.priz.base.application.features.files.converter.FileConverter;
import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileSearchRequest;
import com.priz.base.domain.elasticsearch.file.document.FileDocument;
import com.priz.base.domain.elasticsearch.file.repository.FileDocumentRepository;
import com.priz.base.domain.mysql_priz_base.repository.FileRepository;
import com.priz.common.security.SecurityContextHolder;
import com.priz.interfaces.admin.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSearchServiceImpl implements FileSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final FileRepository fileRepository;

    @Override
    public PageResponse<FileDetailResponse> search(FileSearchRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        
        Criteria criteria = new Criteria("userId").is(userId)
                .and(new Criteria("originalName").contains(request.getQuery())
                        .or(new Criteria("content").contains(request.getQuery())));

        Query query = new CriteriaQuery(criteria)
                .setPageable(request.getPagination().toPageable());

        SearchHits<FileDocument> searchHits = elasticsearchOperations.search(query, FileDocument.class);
        
        List<FileDetailResponse> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> {
                    // We can either map from doc or fetch from MySQL for most up-to-date metadata
                    // Mapping from doc is faster.
                    return FileDetailResponse.builder()
                            .id(doc.getId())
                            .originalName(doc.getOriginalName())
                            .fileType(doc.getFileType())
                            .description(doc.getDescription())
                            .userId(doc.getUserId())
                            .createdAt(doc.getCreatedAt())
                            .updatedAt(doc.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        long totalHits = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalHits / request.getPagination().getSize());

        return PageResponse.<FileDetailResponse>builder()
                .content(content)
                .page(request.getPagination().getPage())
                .pageSize(request.getPagination().getSize())
                .total(totalHits)
                .totalPages(totalPages)
                .first(request.getPagination().getPage() == 1)
                .last(request.getPagination().getPage() >= totalPages)
                .build();
    }
}
