package com.priz.base.application.features.notes.dto;

import com.priz.interfaces.admin.dto.PageRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSearchRequest {

    private String keyword;
    private PageRequestDto pagination;
}
