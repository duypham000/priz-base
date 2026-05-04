package com.priz.base.application.features.notes;

import com.priz.base.application.features.files.dto.FileDetailResponse;
import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.application.features.notes.dto.CreateNoteRequest;
import com.priz.base.application.features.notes.dto.NoteSearchRequest;
import com.priz.base.application.features.notes.dto.UpdateNoteRequest;
import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import com.priz.interfaces.admin.dto.PageResponse;
import org.springframework.data.domain.Page;

public interface NoteService {

    FileDetailResponse createNote(CreateNoteRequest request);

    FileDetailResponse updateNote(String id, UpdateNoteRequest request);

    void deleteNote(String id);

    FileDetailResponse getNoteDetail(String id);

    PageResponse<FileDetailResponse> getNoteList(FileFilterRequest filter);

    PageResponse<NoteDocument> searchNotes(NoteSearchRequest request);

    void reindexAll();
}
