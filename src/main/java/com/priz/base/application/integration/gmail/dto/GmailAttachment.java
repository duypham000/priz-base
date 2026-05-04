package com.priz.base.application.integration.gmail.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GmailAttachment {

    private String fileName;
    private String contentType;
    private long size;
    private byte[] content;
}
