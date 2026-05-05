package com.priz.base.domain.elasticsearch.file.repository;

import com.priz.base.domain.elasticsearch.file.document.FileDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileDocumentRepository extends ElasticsearchRepository<FileDocument, String> {
}
