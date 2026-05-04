package com.priz.base.config.gmail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("gmail")
public class GmailProperties {

    private boolean enabled = false;
    private String username;
    private String appPassword;
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private String imapHost = "imap.gmail.com";
    private int imapPort = 993;
}
