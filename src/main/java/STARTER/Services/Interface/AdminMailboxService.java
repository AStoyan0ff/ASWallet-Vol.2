package STARTER.Services.Interface;

import STARTER.DTOs.AdminRecipientOptionDTO;
import STARTER.DTOs.AdminSendMessageRequest;
import STARTER.DTOs.MailboxMessageViewDTO;
import STARTER.DTOs.UserSendMessageRequest;

import java.util.List;
import java.util.UUID;

public interface AdminMailboxService {

    void sendMessageToUser(String adminUsername, AdminSendMessageRequest request);
    void sendMessageToAdmin(String username, UserSendMessageRequest request);

    List<MailboxMessageViewDTO> listMessagesForUser(String username);
    List<MailboxMessageViewDTO> listThreadForUser(String username);
    List<MailboxMessageViewDTO> listAdminInbox();
    List<AdminRecipientOptionDTO> listAdminRecipients();

    MailboxMessageViewDTO viewMessageForUser(String username, UUID messageId);

    void markUserThreadRead(String adminUsername, String userUsername);
    void markAllUserMailboxRead(String username);
    void deleteUserMailbox(String username);

    long countUnreadForUser(String username);
    long countUnreadForAdminInbox();

    void markAllAdminInboxRead(String adminUsername);
    void deleteAllInboxMessageThreads(String adminUsername);
}
