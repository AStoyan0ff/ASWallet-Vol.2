package STARTER.Services.Impl;

import STARTER.CustomException.CannotChangeAdminRoleException;
import STARTER.CustomException.CannotMessageAdminException;
import STARTER.CustomException.MailboxMessageNotFoundException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.AdminRecipientOptionDTO;
import STARTER.DTOs.AdminSendMessageRequest;
import STARTER.DTOs.MailboxMessageViewDTO;
import STARTER.DTOs.UserSendMessageRequest;
import STARTER.Enums.UserRole;
import STARTER.Models.AdminMailboxMessage;
import STARTER.Models.User;
import STARTER.Repositories.AdminMailboxMessageRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Utils.DateTimeDisplay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminMailboxServiceImpl implements AdminMailboxService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMailboxServiceImpl.class);
    private static final int PREVIEW_MAX_LENGTH = 100;
    private final AdminMailboxMessageRepository mailboxMessageRepository;
    private final UserRepository userRepository;

    @Value("${app.admin.username:admin}")
    private String primaryAdminUsername;

    public AdminMailboxServiceImpl(
            AdminMailboxMessageRepository mailboxMessageRepository,
            UserRepository userRepository) {

        this.mailboxMessageRepository = mailboxMessageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void sendMessageToUser(String adminUsername, AdminSendMessageRequest request) {
        User admin = findUser(adminUsername);
        assertActorIsAdmin(admin);

        User recipient = findUser(request.getUsername().trim());

        if (recipient.getRole() != UserRole.USER) {
            throw new CannotMessageAdminException("Messages can only be sent to regular user accounts.");
        }

        if (isPrimaryAdminAccount(recipient)) {
            throw new CannotMessageAdminException("Messages cannot be sent to the super admin account.");
        }

        AdminMailboxMessage message = AdminMailboxMessage.builder()
            .recipientUserId(recipient.getId())
            .recipientUsername(recipient.getUsername())
            .senderUsername(admin.getUsername())
            .fromAdmin(true)
            .body(request.getMessage().trim())
            .readByRecipient(false)
            .readByAdmin(true)
            .build();

        mailboxMessageRepository.save(message);
        logger.info("Admin sent mailbox message: admin={}, recipientUsername={}", adminUsername, recipient.getUsername());
    }

    @Override
    @Transactional
    public void sendMessageToAdmin(String username, UserSendMessageRequest request) {
        User user = findUser(username);

        if (user.getRole() != UserRole.USER) {
            throw new CannotMessageAdminException("Only regular users can send messages to an admin.");
        }

        User admin = findUser(request.getAdminUsername().trim());
        assertActorIsAdmin(admin);

        AdminMailboxMessage message = AdminMailboxMessage.builder()
            .recipientUserId(user.getId())
            .recipientUsername(user.getUsername())
            .senderUsername(user.getUsername())
            .adminRecipientUsername(admin.getUsername())
            .fromAdmin(false)
            .body(request.getMessage().trim())
            .readByRecipient(true)
            .readByAdmin(false)
            .build();

        mailboxMessageRepository.save(message);
        logger.info("User sent mailbox message to admin: username={}, adminUsername={}", username, admin.getUsername());
    }

    @Override
    public List<MailboxMessageViewDTO> listMessagesForUser(String username) {

        return mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtDesc(username)
            .stream()
            .map(message -> mapToViewForUser(message, username))
            .toList();
    }

    @Override
    public List<MailboxMessageViewDTO> listThreadForUser(String username) {
        findUser(username);

        return mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtAsc(username)
            .stream()
            .map(message -> mapToViewForUser(message, username))
            .toList();
    }

    @Override
    public List<MailboxMessageViewDTO> listAdminInbox() {
        return mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc()

            .stream()
            .map(this::mapToViewForAdminInbox)
            .toList();
    }

    @Override
    public List<AdminRecipientOptionDTO> listAdminRecipients() {

        return userRepository.findAllByRole(UserRole.ADMIN)
            .stream()
            .sorted(Comparator.comparing((User admin) -> !isPrimaryAdminAccount(admin)).reversed()
            .thenComparing(User::getUsername))
            .map(admin -> AdminRecipientOptionDTO.builder()
            .username(admin.getUsername())
            .displayName(resolveAdminDisplayName(admin.getUsername()))
            .build())
            .toList();
    }

    @Override
    @Transactional
    public void markAllUserMailboxRead(String username) {
        findUser(username);

        List<AdminMailboxMessage> unread = mailboxMessageRepository
            .findAllByRecipientUsernameOrderByCreatedAtDesc(username)
            .stream()
            .filter(message -> message.isFromAdmin() && !message.isReadByRecipient())
            .toList();

        for (AdminMailboxMessage message : unread) {
            message.setReadByRecipient(true);
        }

        mailboxMessageRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteUserMailbox(String username) {
        User user = findUser(username);

        if (user.getRole() != UserRole.USER) {
            throw new CannotMessageAdminException("Only regular users can delete their mailbox.");
        }

        mailboxMessageRepository.deleteByRecipientUsername(username);
        logger.info("User mailbox deleted: username={}", username);
    }

    @Override
    @Transactional
    public MailboxMessageViewDTO viewMessageForUser(String username, UUID messageId) {

        AdminMailboxMessage message = mailboxMessageRepository
            .findByIdAndRecipientUsername(messageId, username)
            .orElseThrow(() -> new MailboxMessageNotFoundException("Message not found."));

        if (message.isFromAdmin() && !message.isReadByRecipient()) {
            message.setReadByRecipient(true);
            mailboxMessageRepository.save(message);
        }

        return mapToViewForUser(message, username);
    }

    @Override
    @Transactional
    public void markUserThreadRead(String adminUsername, String userUsername) {
        User admin = findUser(adminUsername);

        assertActorIsAdmin(admin);
        findUser(userUsername);

        List<AdminMailboxMessage> unread = mailboxMessageRepository
            .findAllByRecipientUsernameOrderByCreatedAtAsc(userUsername)
            .stream()
            .filter(message -> !message.isFromAdmin() && !message.isReadByAdmin())
            .toList();

        for (AdminMailboxMessage message : unread) {
            message.setReadByAdmin(true);
        }

        mailboxMessageRepository.saveAll(unread);
    }

    @Override
    public long countUnreadForUser(String username) {
        return mailboxMessageRepository.countByRecipientUsernameAndFromAdminTrueAndReadByRecipientFalse(username);
    }

    @Override
    public long countUnreadForAdminInbox() {
        return mailboxMessageRepository.countByFromAdminFalseAndReadByAdminFalse();
    }

    @Override
    @Transactional
    public void markAllAdminInboxRead(String adminUsername) {

        User admin = findUser(adminUsername);
        assertActorIsAdmin(admin);

        List<AdminMailboxMessage> unread = mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc()
            .stream()
            .filter(message -> !message.isReadByAdmin())
            .toList();

        for (AdminMailboxMessage message : unread) {
            message.setReadByAdmin(true);
        }

        mailboxMessageRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteAllInboxMessageThreads(String adminUsername) {

        User admin = findUser(adminUsername);
        assertActorIsAdmin(admin);

        mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc()
            .stream()
            .map(AdminMailboxMessage::getSenderUsername)
            .distinct()
            .forEach(mailboxMessageRepository::deleteByRecipientUsername);
    }

    private void assertActorIsAdmin(User admin) {

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CannotChangeAdminRoleException(
                "You do not have admin permissions. Please log out and log in again.");
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
            new UserNotFoundException("User not found"));
    }

    private boolean isPrimaryAdminAccount(User user) {
        return primaryAdminUsername.trim().equals(user.getUsername());
    }

    private MailboxMessageViewDTO mapToViewForUser(AdminMailboxMessage message, String viewerUsername) {
        return MailboxMessageViewDTO.builder()

            .id(message.getId())
            .senderUsername(message.getSenderUsername())
            .senderDisplayName(resolveSenderDisplayForUser(message))
            .adminRecipientUsername(message.getAdminRecipientUsername())
            .adminRecipientDisplayName(resolveAdminDisplayName(message.getAdminRecipientUsername()))
            .fromAdmin(message.isFromAdmin())
            .body(message.getBody())
            .preview(buildPreview(message.getBody()))
            .createdAt(DateTimeDisplay.format(message.getCreatedAt()))
            .readByRecipient(message.isReadByRecipient())
            .readByAdmin(message.isReadByAdmin())
            .build();
    }

    private MailboxMessageViewDTO mapToViewForAdminInbox(AdminMailboxMessage message) {
        return MailboxMessageViewDTO.builder()

            .id(message.getId())
            .senderUsername(message.getSenderUsername())
            .senderDisplayName(message.getSenderUsername())
            .adminRecipientUsername(message.getAdminRecipientUsername())
            .adminRecipientDisplayName(resolveAdminDisplayName(message.getAdminRecipientUsername()))
            .fromAdmin(false)
            .body(message.getBody())
            .preview(buildPreview(message.getBody()))
            .createdAt(DateTimeDisplay.format(message.getCreatedAt()))
            .readByRecipient(message.isReadByRecipient())
            .readByAdmin(message.isReadByAdmin())
            .build();
    }

    private String resolveSenderDisplayForUser(AdminMailboxMessage message) {

        if (!message.isFromAdmin()) {
            return "You";
        }

        return resolveAdminDisplayName(message.getSenderUsername());
    }

    private String resolveAdminDisplayName(String username) {

        if (username == null || username.isBlank()) {
            return "Admin";
        }

        if (isPrimaryAdminAccount(username)) {
            return "SUPER ADMIN";
        }

        return "SUPPORT ADMIN";
    }

    private boolean isPrimaryAdminAccount(String username) {
        return primaryAdminUsername.trim().equals(username);
    }

    private String buildPreview(String body) {

        if (body == null || body.isBlank()) {
            return "";
        }

        String normalized = body.trim().replaceAll("\\s+", " ");

        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, PREVIEW_MAX_LENGTH) + "…";
    }
}
