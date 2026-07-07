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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMailboxServiceImplTest {

    @Mock private AdminMailboxMessageRepository mailboxMessageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AdminMailboxServiceImpl adminMailboxService;

    private User admin;
    private User supportAdmin;
    private User user;
    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminMailboxService, "primaryAdminUsername", "admin");

        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        user = buildUser(userId, "Plamen", UserRole.USER);
        admin = buildUser(adminId, "admin", UserRole.ADMIN);
        supportAdmin = buildUser(UUID.randomUUID(), "support", UserRole.ADMIN);
    }

    // --- SEND MESSAGE TO USER ---

    @Test
    void sendMessageToUser_success_savesMessageForRegularUser() {
        AdminSendMessageRequest request = new AdminSendMessageRequest();
        request.setUsername("Plamen");
        request.setMessage("  Hello there  ");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));

        adminMailboxService.sendMessageToUser("admin", request);

        ArgumentCaptor<AdminMailboxMessage> captor = ArgumentCaptor.forClass(AdminMailboxMessage.class);
        verify(mailboxMessageRepository).save(captor.capture());

        AdminMailboxMessage saved = captor.getValue();
        assertThat(saved.getRecipientUserId()).isEqualTo(userId);
        assertThat(saved.getRecipientUsername()).isEqualTo("Plamen");
        assertThat(saved.getSenderUsername()).isEqualTo("admin");
        assertThat(saved.getBody()).isEqualTo("Hello there");
        assertThat(saved.isFromAdmin()).isTrue();
        assertThat(saved.isReadByRecipient()).isFalse();
        assertThat(saved.isReadByAdmin()).isTrue();
    }

    @Test
    void sendMessageToUser_nonAdmin_throws() {
        AdminSendMessageRequest request = new AdminSendMessageRequest();
        request.setUsername("Plamen");
        request.setMessage("Hello");

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminMailboxService.sendMessageToUser("Plamen", request));

        verify(mailboxMessageRepository, never()).save(any());
    }

    @Test
    void sendMessageToUser_adminRecipient_throws() {

        AdminSendMessageRequest request = new AdminSendMessageRequest();
        request.setUsername("support");
        request.setMessage("Hello");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));

        assertThrows(CannotMessageAdminException.class,
                () -> adminMailboxService.sendMessageToUser("admin", request));

        verify(mailboxMessageRepository, never()).save(any());
    }

    // --- SEND MESSAGE TO ADMIN ---

    @Test
    void sendMessageToAdmin_success_savesUserReply() {
        UserSendMessageRequest request = new UserSendMessageRequest();
        request.setAdminUsername("support");
        request.setMessage("  Need help  ");

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));

        adminMailboxService.sendMessageToAdmin("Plamen", request);

        ArgumentCaptor<AdminMailboxMessage> captor = ArgumentCaptor.forClass(AdminMailboxMessage.class);
        verify(mailboxMessageRepository).save(captor.capture());

        AdminMailboxMessage saved = captor.getValue();
        assertThat(saved.getRecipientUsername()).isEqualTo("Plamen");
        assertThat(saved.getSenderUsername()).isEqualTo("Plamen");
        assertThat(saved.getAdminRecipientUsername()).isEqualTo("support");
        assertThat(saved.getBody()).isEqualTo("Need help");
        assertThat(saved.isFromAdmin()).isFalse();
        assertThat(saved.isReadByRecipient()).isTrue();
        assertThat(saved.isReadByAdmin()).isFalse();
    }

    @Test
    void sendMessageToAdmin_adminUser_throws() {

        UserSendMessageRequest request = new UserSendMessageRequest();
        request.setAdminUsername("admin");
        request.setMessage("Hello");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThrows(CannotMessageAdminException.class,
                () -> adminMailboxService.sendMessageToAdmin("admin", request));

        verify(mailboxMessageRepository, never()).save(any());
    }

    // --- LIST / MAP ---

    @Test
    void listMessagesForUser_mapsPreviewAndDisplayNames() {
        AdminMailboxMessage fromAdmin = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "admin",
                true,
                "Short admin message",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        AdminMailboxMessage fromUser = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "x".repeat(120),
                LocalDateTime.of(2026, 7, 7, 11, 0)
        );
        fromUser.setAdminRecipientUsername("support");

        when(mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtDesc("Plamen"))
                .thenReturn(List.of(fromAdmin, fromUser));

        List<MailboxMessageViewDTO> result = adminMailboxService.listMessagesForUser("Plamen");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSenderDisplayName()).isEqualTo("SUPER ADMIN");
        assertThat(result.get(0).getPreview()).isEqualTo("Short admin message");
        assertThat(result.get(1).getSenderDisplayName()).isEqualTo("You");
        assertThat(result.get(1).getAdminRecipientDisplayName()).isEqualTo("SUPPORT ADMIN");
        assertThat(result.get(1).getPreview()).hasSize(101).endsWith("…");
    }

    @Test
    void listThreadForUser_returnsMappedThread() {
        AdminMailboxMessage message = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "admin",
                true,
                "Thread message",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtAsc("Plamen"))
                .thenReturn(List.of(message));

        List<MailboxMessageViewDTO> result = adminMailboxService.listThreadForUser("Plamen");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBody()).isEqualTo("Thread message");
    }

    @Test
    void listAdminInbox_mapsUserReplies() {
        AdminMailboxMessage reply = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "Please help",
                LocalDateTime.of(2026, 7, 7, 12, 0)
        );
        reply.setAdminRecipientUsername("admin");

        when(mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(reply));

        List<MailboxMessageViewDTO> result = adminMailboxService.listAdminInbox();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isFromAdmin()).isFalse();
        assertThat(result.getFirst().getSenderDisplayName()).isEqualTo("Plamen");
        assertThat(result.getFirst().getAdminRecipientDisplayName()).isEqualTo("SUPER ADMIN");
    }

    @Test
    void listAdminRecipients_mapsDisplayNames() {
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(supportAdmin, admin));

        List<AdminRecipientOptionDTO> result = adminMailboxService.listAdminRecipients();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdminRecipientOptionDTO::getUsername)
                .containsExactly("support", "admin");
        assertThat(result).extracting(AdminRecipientOptionDTO::getDisplayName)
                .containsExactly("SUPPORT ADMIN", "SUPER ADMIN");
    }

    // --- RAED / UNREAD ---

    @Test
    void viewMessageForUser_marksUnreadAdminMessageAsRead() {
        UUID messageId = UUID.randomUUID();
        AdminMailboxMessage message = buildMessage(
                messageId,
                "Plamen",
                "admin",
                true,
                "Unread",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        message.setReadByRecipient(false);

        when(mailboxMessageRepository.findByIdAndRecipientUsername(messageId, "Plamen"))
                .thenReturn(Optional.of(message));

        MailboxMessageViewDTO view = adminMailboxService.viewMessageForUser("Plamen", messageId);

        assertThat(view.getBody()).isEqualTo("Unread");
        assertThat(message.isReadByRecipient()).isTrue();
        verify(mailboxMessageRepository).save(message);
    }

    @Test
    void viewMessageForUser_unknownMessage_throws() {
        UUID messageId = UUID.randomUUID();
        when(mailboxMessageRepository.findByIdAndRecipientUsername(messageId, "Plamen"))
                .thenReturn(Optional.empty());

        assertThrows(MailboxMessageNotFoundException.class,
                () -> adminMailboxService.viewMessageForUser("Plamen", messageId));
    }

    @Test
    void markAllUserMailboxRead_marksUnreadAdminMessages() {
        AdminMailboxMessage unread = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "admin",
                true,
                "Hello",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        unread.setReadByRecipient(false);
        AdminMailboxMessage alreadyRead = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "admin",
                true,
                "Old",
                LocalDateTime.of(2026, 7, 6, 10, 0)
        );
        alreadyRead.setReadByRecipient(true);
        AdminMailboxMessage userReply = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "Reply",
                LocalDateTime.of(2026, 7, 7, 11, 0)
        );

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtDesc("Plamen"))
                .thenReturn(List.of(unread, alreadyRead, userReply));

        adminMailboxService.markAllUserMailboxRead("Plamen");

        assertThat(unread.isReadByRecipient()).isTrue();
        verify(mailboxMessageRepository).saveAll(List.of(unread));
    }

    @Test
    void markUserThreadRead_marksUnreadUserMessagesForAdmin() {
        AdminMailboxMessage unread = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "Need help",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        unread.setReadByAdmin(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(mailboxMessageRepository.findAllByRecipientUsernameOrderByCreatedAtAsc("Plamen"))
                .thenReturn(List.of(unread));

        adminMailboxService.markUserThreadRead("admin", "Plamen");

        assertThat(unread.isReadByAdmin()).isTrue();
        verify(mailboxMessageRepository).saveAll(List.of(unread));
    }

    @Test
    void markAllAdminInboxRead_marksUnreadReplies() {
        AdminMailboxMessage unread = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "Unread reply",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        unread.setReadByAdmin(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(unread));

        adminMailboxService.markAllAdminInboxRead("admin");

        assertThat(unread.isReadByAdmin()).isTrue();
        verify(mailboxMessageRepository).saveAll(List.of(unread));
    }

    @Test
    void countUnreadForUser_delegatesToRepository() {
        when(mailboxMessageRepository.countByRecipientUsernameAndFromAdminTrueAndReadByRecipientFalse("Plamen"))
                .thenReturn(2L);

        assertThat(adminMailboxService.countUnreadForUser("Plamen")).isEqualTo(2L);
    }

    @Test
    void countUnreadForAdminInbox_delegatesToRepository() {
        when(mailboxMessageRepository.countByFromAdminFalseAndReadByAdminFalse()).thenReturn(5L);

        assertThat(adminMailboxService.countUnreadForAdminInbox()).isEqualTo(5L);
    }

    // --- DELETE ---

    @Test
    void deleteUserMailbox_success_deletesByUsername() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));

        adminMailboxService.deleteUserMailbox("Plamen");

        verify(mailboxMessageRepository).deleteByRecipientUsername("Plamen");
    }

    @Test
    void deleteUserMailbox_adminUser_throws() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThrows(CannotMessageAdminException.class,
                () -> adminMailboxService.deleteUserMailbox("admin"));

        verify(mailboxMessageRepository, never()).deleteByRecipientUsername(any());
    }

    @Test
    void deleteAllInboxMessageThreads_deletesDistinctUserThreads() {
        AdminMailboxMessage fromPlamen = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "One",
                LocalDateTime.of(2026, 7, 7, 10, 0)
        );
        AdminMailboxMessage fromGeorgi = buildMessage(
                UUID.randomUUID(),
                "Georgi",
                "Georgi",
                false,
                "Two",
                LocalDateTime.of(2026, 7, 7, 11, 0)
        );
        AdminMailboxMessage fromPlamenAgain = buildMessage(
                UUID.randomUUID(),
                "Plamen",
                "Plamen",
                false,
                "Three",
                LocalDateTime.of(2026, 7, 7, 12, 0)
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(mailboxMessageRepository.findAllByFromAdminFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(fromPlamen, fromGeorgi, fromPlamenAgain));

        adminMailboxService.deleteAllInboxMessageThreads("admin");

        verify(mailboxMessageRepository).deleteByRecipientUsername("Plamen");
        verify(mailboxMessageRepository).deleteByRecipientUsername("Georgi");
    }

    @Test
    void findUser_unknownUsername_throws() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> adminMailboxService.listThreadForUser("missing"));
    }

    private User buildUser(UUID id, String username, UserRole role) {
        User built = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("encoded")
                .role(role)
                .build();
        built.setId(id);
        return built;
    }

    private AdminMailboxMessage buildMessage(
            UUID id,
            String recipientUsername,
            String senderUsername,
            boolean fromAdmin,
            String body,
            LocalDateTime createdAt
    ) {
        AdminMailboxMessage message = AdminMailboxMessage.builder()
                .recipientUserId(userId)
                .recipientUsername(recipientUsername)
                .senderUsername(senderUsername)
                .fromAdmin(fromAdmin)
                .body(body)
                .build();
        message.setId(id);
        message.setCreatedAt(createdAt);
        return message;
    }
}
