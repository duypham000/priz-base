package com.priz.base.interfaces.rest;

import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.notes.NoteService;
import com.priz.base.application.features.notes.dto.CreateNoteRequest;
import com.priz.base.application.features.notes.dto.NoteSearchRequest;
import com.priz.base.application.features.notes.dto.UpdateNoteRequest;
import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import com.priz.base.common.response.ApiResponse;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.common.security.annotation.Secured;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @Secured
    public ApiResponse<FileDetailResponse> createNote(@Valid @RequestBody CreateNoteRequest request) {
        return ApiResponse.success(noteService.createNote(request));
    }

    @GetMapping("/{id}")
    @Secured
    public ApiResponse<FileDetailResponse> getNoteDetail(@PathVariable String id) {
        return ApiResponse.success(noteService.getNoteDetail(id));
    }

    @PutMapping("/{id}")
    @Secured
    public ApiResponse<FileDetailResponse> updateNote(
            @PathVariable String id,
            @Valid @RequestBody UpdateNoteRequest request) {
        return ApiResponse.success(noteService.updateNote(id, request));
    }

    @DeleteMapping("/{id}")
    @Secured
    public ApiResponse<Void> deleteNote(@PathVariable String id) {
        noteService.deleteNote(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/filter")
    @Secured
    public ApiResponse<PageResponse<FileDetailResponse>> getNoteList(@RequestBody FileFilterRequest filter) {
        return ApiResponse.success(noteService.getNoteList(filter));
    }

    @PostMapping("/search")
    @Secured
    public ApiResponse<PageResponse<NoteDocument>> searchNotes(@RequestBody NoteSearchRequest request) {
        return ApiResponse.success(noteService.searchNotes(request));
    }

    @PostMapping("/reindex")
    @Secured(roles = {"ADMIN"})
    public ApiResponse<Void> reindexAll() {
        noteService.reindexAll();
        return ApiResponse.success(null);
    }
}
