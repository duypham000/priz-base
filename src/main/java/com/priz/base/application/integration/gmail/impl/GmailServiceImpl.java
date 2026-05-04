package com.priz.base.application.integration.gmail.impl;

import com.priz.base.application.integration.gmail.GmailService;
import com.priz.base.application.integration.gmail.dto.GmailAttachment;
import com.priz.base.application.integration.gmail.dto.GmailMessage;
import com.priz.base.application.integration.gmail.dto.GmailSendRequest;
import com.priz.base.config.gmail.GmailProperties;
import com.priz.common.exception.BusinessException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.OrTerm;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class GmailServiceImpl implements GmailService {

    private final JavaMailSender mailSender;
    private final GmailProperties props;

    public GmailServiceImpl(JavaMailSender mailSender, GmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    // ── Gửi mail ──────────────────────────────────────────────────────────────

    @Override
    public void sendEmail(String to, String subject, String body) {
        sendEmail(GmailSendRequest.builder()
                .to(List.of(to))
                .subject(subject)
                .body(body)
                .html(false)
                .build());
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendEmail(GmailSendRequest.builder()
                .to(List.of(to))
                .subject(subject)
                .body(htmlBody)
                .html(true)
                .build());
    }

    @Override
    public void sendEmail(GmailSendRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            applyRecipients(helper, request);
            helper.setText(request.getBody(), request.isHtml());
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_SEND_ERROR", "Gửi email thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailWithAttachments(GmailSendRequest request, List<MultipartFile> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            applyRecipients(helper, request);
            helper.setText(request.getBody(), request.isHtml());
            for (MultipartFile file : attachments) {
                helper.addAttachment(
                        file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                        file
                );
            }
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_SEND_ERROR", "Gửi email kèm file thất bại: " + e.getMessage(), e);
        }
    }

    // ── Đọc / liệt kê ─────────────────────────────────────────────────────────

    @Override
    public List<GmailMessage> getEmails(String folder, int limit) {
        try (Store store = connectImap(); Folder mailFolder = openFolder(store, folder, Folder.READ_ONLY)) {
            int total = mailFolder.getMessageCount();
            int start = Math.max(1, total - limit + 1);
            Message[] messages = mailFolder.getMessages(start, total);
            List<GmailMessage> result = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0; i--) {
                result.add(parseMessage(mailFolder, messages[i]));
            }
            return result;
        } catch (MessagingException | IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_READ_ERROR", "Đọc email thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public GmailMessage getEmail(String folder, long uid) {
        try (Store store = connectImap(); Folder mailFolder = openFolder(store, folder, Folder.READ_ONLY)) {
            UIDFolder uidFolder = (UIDFolder) mailFolder;
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_NOT_FOUND", "Không tìm thấy email với UID: " + uid);
            }
            return parseMessage(mailFolder, message);
        } catch (BusinessException e) {
            throw e;
        } catch (MessagingException | IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_READ_ERROR", "Đọc email thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GmailMessage> searchEmails(String query, int limit) {
        try (Store store = connectImap(); Folder mailFolder = openFolder(store, "INBOX", Folder.READ_ONLY)) {
            SearchTerm term = new OrTerm(new SubjectTerm(query), new FromStringTerm(query));
            Message[] messages = mailFolder.search(term);
            List<GmailMessage> result = new ArrayList<>();
            int start = Math.max(0, messages.length - limit);
            for (int i = messages.length - 1; i >= start; i--) {
                result.add(parseMessage(mailFolder, messages[i]));
            }
            return result;
        } catch (MessagingException | IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_SEARCH_ERROR", "Tìm kiếm email thất bại: " + e.getMessage(), e);
        }
    }

    // ── Quản lý ────────────────────────────────────────────────────────────────

    @Override
    public void markAsRead(String folder, long uid) {
        setFlag(folder, uid, Flags.Flag.SEEN, true);
    }

    @Override
    public void markAsUnread(String folder, long uid) {
        setFlag(folder, uid, Flags.Flag.SEEN, false);
    }

    @Override
    public void deleteEmail(String folder, long uid) {
        try (Store store = connectImap(); Folder mailFolder = openFolder(store, folder, Folder.READ_WRITE)) {
            UIDFolder uidFolder = (UIDFolder) mailFolder;
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_NOT_FOUND", "Không tìm thấy email với UID: " + uid);
            }
            message.setFlag(Flags.Flag.DELETED, true);
            mailFolder.expunge();
        } catch (BusinessException e) {
            throw e;
        } catch (MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_DELETE_ERROR", "Xóa email thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void moveEmail(String folder, long uid, String targetFolder) {
        Store store = null;
        Folder source = null;
        Folder target = null;
        try {
            store = connectImap();
            source = openFolder(store, folder, Folder.READ_WRITE);
            target = store.getFolder(targetFolder);

            if (!target.exists()) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_FOLDER_NOT_FOUND", "Folder không tồn tại: " + targetFolder);
            }
            target.open(Folder.READ_WRITE);

            UIDFolder uidFolder = (UIDFolder) source;
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_NOT_FOUND", "Không tìm thấy email với UID: " + uid);
            }

            source.copyMessages(new Message[]{message}, target);
            try {
                // Gmail tự remove nguồn khi copy vào Trash — ignore nếu đã bị remove
                message.setFlag(Flags.Flag.DELETED, true);
                source.expunge();
            } catch (jakarta.mail.MessageRemovedException ignored) {}
        } catch (BusinessException e) {
            throw e;
        } catch (MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_MOVE_ERROR", "Di chuyển email thất bại: " + e.getMessage(), e);
        } finally {
            closeQuietly(target);
            closeQuietly(source);
            closeQuietly(store);
        }
    }

    @Override
    public List<String> getFolders() {
        try (Store store = connectImap()) {
            Folder[] folders = store.getDefaultFolder().list("*");
            return Arrays.stream(folders).map(Folder::getFullName).toList();
        } catch (MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_FOLDER_ERROR", "Lấy danh sách folder thất bại: " + e.getMessage(), e);
        }
    }

    // ── Health ─────────────────────────────────────────────────────────────────

    @Override
    public boolean isAvailable() {
        try (Store store = connectImap()) {
            return store.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Store connectImap() throws MessagingException {
        Properties imapProps = new Properties();
        imapProps.put("mail.store.protocol", "imaps");
        imapProps.put("mail.imaps.host", props.getImapHost());
        imapProps.put("mail.imaps.port", String.valueOf(props.getImapPort()));
        imapProps.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(imapProps);
        Store store = session.getStore("imaps");
        store.connect(props.getImapHost(), props.getImapPort(), props.getUsername(), props.getAppPassword());
        return store;
    }

    private Folder openFolder(Store store, String folderName, int mode) throws MessagingException {
        Folder folder = store.getFolder(folderName);
        if (!folder.exists()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_FOLDER_NOT_FOUND", "Folder không tồn tại: " + folderName);
        }
        folder.open(mode);
        return folder;
    }

    private void setFlag(String folderName, long uid, Flags.Flag flag, boolean value) {
        try (Store store = connectImap(); Folder mailFolder = openFolder(store, folderName, Folder.READ_WRITE)) {
            UIDFolder uidFolder = (UIDFolder) mailFolder;
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "GMAIL_NOT_FOUND", "Không tìm thấy email với UID: " + uid);
            }
            message.setFlag(flag, value);
        } catch (BusinessException e) {
            throw e;
        } catch (MessagingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GMAIL_FLAG_ERROR", "Cập nhật flag email thất bại: " + e.getMessage(), e);
        }
    }

    private GmailMessage parseMessage(Folder folder, Message message) throws MessagingException, IOException {
        UIDFolder uidFolder = (UIDFolder) folder;
        long uid = uidFolder.getUID(message);

        String from = message.getFrom() != null && message.getFrom().length > 0
                ? message.getFrom()[0].toString() : null;
        String to = addressList(message.getRecipients(Message.RecipientType.TO));
        String cc = addressList(message.getRecipients(Message.RecipientType.CC));
        String subject = message.getSubject();
        boolean read = message.isSet(Flags.Flag.SEEN);
        LocalDateTime receivedAt = toLocalDateTime(message.getReceivedDate());
        LocalDateTime sentAt = toLocalDateTime(message.getSentDate());
        String messageId = message instanceof MimeMessage mm ? mm.getMessageID() : null;

        String plainBody = null;
        String htmlBody = null;
        List<GmailAttachment> attachments = new ArrayList<>();

        Object content = message.getContent();
        if (content instanceof String text) {
            if ("text/html".equalsIgnoreCase(message.getContentType().split(";")[0].trim())) {
                htmlBody = text;
            } else {
                plainBody = text;
            }
        } else if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String disposition = part.getDisposition();
                String partType = part.getContentType().split(";")[0].trim().toLowerCase();

                if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition) && part.getFileName() != null) {
                    attachments.add(GmailAttachment.builder()
                            .fileName(part.getFileName())
                            .contentType(partType)
                            .size(part.getSize())
                            .content(part.getInputStream().readAllBytes())
                            .build());
                } else if ("text/plain".equals(partType)) {
                    plainBody = (String) part.getContent();
                } else if ("text/html".equals(partType)) {
                    htmlBody = (String) part.getContent();
                }
            }
        }

        return GmailMessage.builder()
                .uid(uid)
                .messageId(messageId)
                .from(from)
                .to(to)
                .cc(cc)
                .subject(subject)
                .body(plainBody)
                .htmlBody(htmlBody)
                .receivedAt(receivedAt)
                .sentAt(sentAt)
                .read(read)
                .attachments(attachments)
                .build();
    }

    private void applyRecipients(MimeMessageHelper helper, GmailSendRequest request) throws MessagingException {
        if (request.getTo() != null) {
            helper.setTo(request.getTo().toArray(new String[0]));
        }
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(new String[0]));
        }
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(new String[0]));
        }
        if (request.getReplyTo() != null) {
            helper.setReplyTo(request.getReplyTo());
        }
        helper.setSubject(request.getSubject() != null ? request.getSubject() : "");
        helper.setFrom(new InternetAddress(props.getUsername()));
    }

    private String addressList(jakarta.mail.Address[] addresses) {
        if (addresses == null || addresses.length == 0) return null;
        return Arrays.stream(addresses).map(jakarta.mail.Address::toString).reduce((a, b) -> a + ", " + b).orElse(null);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void closeQuietly(Folder folder) {
        if (folder != null && folder.isOpen()) {
            try { folder.close(false); } catch (MessagingException ignored) {}
        }
    }

    private void closeQuietly(Store store) {
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (MessagingException ignored) {}
        }
    }

}
