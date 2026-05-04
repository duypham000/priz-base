package com.priz.base.domain.elasticsearch.note.repository;

import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NoteSearchRepository extends ElasticsearchRepository<NoteDocument, String> {
    Page<NoteDocument> findByUserIdAndContentContaining(
            String userId, String keyword, Pageable pageable);
}
