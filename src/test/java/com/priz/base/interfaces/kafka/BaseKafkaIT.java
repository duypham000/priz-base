package com.priz.base.interfaces.kafka;

import com.priz.base.application.features.notification.NotificationService;
import com.priz.base.application.integration.discord.DiscordService;
import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.application.integration.telegram.TelegramService;
import com.priz.base.common.storage.LocalStorageService;
import com.priz.base.domain.elasticsearch.note.repository.NoteSearchRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseKafkaIT {

    @MockitoBean
    protected TelegramService telegramService;

    @MockitoBean
    protected GmailService gmailService;

    @MockitoBean
    protected DiscordService discordService;

    @MockitoBean
    protected LocalStorageService storageService;

    // ES không chạy trong test — mock repository để NoteServiceImpl khởi tạo được
    @MockitoBean
    protected NoteSearchRepository noteSearchRepository;

    @MockitoBean
    protected com.priz.base.domain.elasticsearch.file.repository.FileDocumentRepository fileDocumentRepository;

    @MockitoBean
    protected org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    // We don't mock NotificationService here if we want to test the real one, 
    // but some tests might need it mocked if they test FileUpload which calls NotificationService.
    // Let's decide case by case or just mock it here if it's always an "external" integration point.
}
