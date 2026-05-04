package com.priz.base.application.features.notes.impl;

import com.priz.base.application.features.files.converter.FileConverter;
import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.files.helper.FileSpecification;
import com.priz.base.application.features.notes.NoteService;
import com.priz.base.application.features.notes.converter.NoteConverter;
import com.priz.base.application.features.notes.dto.CreateNoteRequest;
import com.priz.base.application.features.notes.dto.NoteSearchRequest;
import com.priz.base.application.features.notes.dto.UpdateNoteRequest;
import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import com.priz.base.domain.elasticsearch.note.repository.NoteSearchRepository;
import com.priz.base.domain.mysql.priz_base.model.FileModel;
import com.priz.base.domain.mysql.priz_base.repository.FileRepository;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final FileRepository fileRepository;
    private final NoteSearchRepository noteSearchRepository;

    @Override
    @Transactional
    public FileDetailResponse createNote(CreateNoteRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();

        FileModel model = FileModel.builder()
                .originalName(request.getTitle())
                .storedName("note-" + UUID.randomUUID())
                .filePath("/notes") // Placeholder
                .fileType("NOTE")
                .content(request.getContent())
                .userId(userId)
                .description(request.getDescription())
                .isSynced(false)
                .build();

        model = fileRepository.save(model);
        indexNote(model);

        log.info("Note created: id={}, userId={}", model.getId(), userId);
        return FileConverter.toDetailResponse(model);
    }

    @Override
    @Transactional
    public FileDetailResponse updateNote(String id, UpdateNoteRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel model = findNoteByIdAndUser(id, userId);

        if (request.getTitle() != null) {
            model.setOriginalName(request.getTitle());
        }
        if (request.getContent() != null) {
            model.setContent(request.getContent());
        }
        if (request.getDescription() != null) {
            model.setDescription(request.getDescription());
        }

        model = fileRepository.save(model);
        indexNote(model);

        log.info("Note updated: id={}, userId={}", id, userId);
        return FileConverter.toDetailResponse(model);
    }

    @Override
    @Transactional
    public void deleteNote(String id) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel model = findNoteByIdAndUser(id, userId);

        fileRepository.delete(model);
        try {
            noteSearchRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("Failed to delete note from ES: {}", id, e);
        }

        log.info("Note deleted: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDetailResponse getNoteDetail(String id) {
        String userId = SecurityContextHolder.getCurrentUserId();
        FileModel model = findNoteByIdAndUser(id, userId);
        return FileConverter.toDetailResponse(model);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileDetailResponse> getNoteList(FileFilterRequest filter) {
        String userId = SecurityContextHolder.getCurrentUserId();

        // Cần đảm bảo FileSpecification hỗ trợ lọc theo fileType
        Specification<FileModel> spec = FileSpecification.withFilters(userId, filter)
                .and((root, query, cb) -> cb.equal(root.get("fileType"), "NOTE"));

        Pageable pageable = filter.getPagination() != null 
                ? filter.getPagination().toPageable() 
                : PageRequest.of(0, 10);
        Page<FileModel> page = fileRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(FileConverter::toDetailResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoteDocument> searchNotes(NoteSearchRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        Pageable pageable = request.getPagination() != null 
                ? request.getPagination().toPageable() 
                : PageRequest.of(0, 10);
        Page<NoteDocument> page = noteSearchRepository.findByUserIdAndContentContaining(
                userId, request.getKeyword(), pageable);
        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public void reindexAll() {
        noteSearchRepository.deleteAll();
        int page = 0;
        Page<FileModel> batch;
        do {
            // Cần một specification hoặc method repository để lấy tất cả NOTE
            batch = fileRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("fileType"), "NOTE"),
                    PageRequest.of(page++, 100));
            
            List<NoteDocument> docs = batch.getContent().stream()
                    .map(NoteConverter::toDocument)
                    .toList();
            
            noteSearchRepository.saveAll(docs);
            log.info("Reindexed batch {}/{}", page, batch.getTotalPages());
        } while (batch.hasNext());
    }

    private void indexNote(FileModel model) {
        try {
            noteSearchRepository.save(NoteConverter.toDocument(model));
        } catch (Exception e) {
            log.warn("ES indexing failed for note {}: {}", model.getId(), e.getMessage());
        }
    }

    private FileModel findNoteByIdAndUser(String id, String userId) {
        FileModel model = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", id));

        if (!model.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this note");
        }

        if (!"NOTE".equals(model.getFileType())) {
            throw new ResourceNotFoundException("Note", "id", id);
        }

        return model;
    }
}
