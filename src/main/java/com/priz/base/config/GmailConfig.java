package com.priz.base.config;

import com.priz.base.application.integration.gmail.impl.GmailServiceImpl;
import com.priz.base.config.gmail.GmailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(prefix = "gmail", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GmailProperties.class)
public class GmailConfig {

    @Bean
    public JavaMailSender gmailMailSender(GmailProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getSmtpHost());
        sender.setPort(props.getSmtpPort());
        sender.setUsername(props.getUsername());
        sender.setPassword(props.getAppPassword());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.smtp.starttls.required", "true");
        javaMailProps.put("mail.debug", "false");

        return sender;
    }

    @Bean
    public GmailServiceImpl gmailService(JavaMailSender gmailMailSender, GmailProperties props) {
        return new GmailServiceImpl(gmailMailSender, props);
    }
}
