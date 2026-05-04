package com.priz.base.application.integration.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GmailSendRequest {

    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private boolean html;
    private String replyTo;
}
