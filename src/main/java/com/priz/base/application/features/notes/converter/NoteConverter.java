package com.priz.base.application.features.notes.converter;

import com.priz.base.domain.elasticsearch.note.document.NoteDocument;
import com.priz.base.domain.mysql.priz_base.model.FileModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoteConverter {

    public static NoteDocument toDocument(FileModel model) {
        if (model == null) {
            return null;
        }

        return NoteDocument.builder()
                .id(model.getId())
                .title(model.getOriginalName())
                .content(model.getContent())
                .userId(model.getUserId())
                .description(model.getDescription())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .tags(new ArrayList<>()) // Khởi tạo list trống
                .build();
    }
}
