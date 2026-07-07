package STARTER.Controllers;

import STARTER.DTOs.AdminRecipientOptionDTO;
import STARTER.DTOs.MailboxMessageViewDTO;
import STARTER.DTOs.UserSendMessageRequest;
import STARTER.Services.Interface.AdminMailboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WalletMailboxController.class)
class WalletMailboxControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMailboxService adminMailboxService;

    @Test
    void getSendMessagePage_regularUser_returnsViewWithRecipients() throws Exception {
        AdminRecipientOptionDTO recipient = AdminRecipientOptionDTO.builder()
                .username("admin")
                .displayName("SUPER ADMIN")
                .build();

        when(adminMailboxService.listAdminRecipients()).thenReturn(List.of(recipient));

        mockMvc.perform(get("/wallet/messages/send")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet-send-message"))
                .andExpect(model().attribute("currentUsername", "Plamen"))
                .andExpect(model().attributeExists("userSendMessageRequest"))
                .andExpect(model().attributeExists("adminRecipients"));
    }

    @Test
    void getSendMessagePage_admin_redirectsToAdminSendPage() throws Exception {
        mockMvc.perform(get("/wallet/messages/send")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/send"));

        verify(adminMailboxService, never()).listAdminRecipients();
    }

    @Test
    void postSendMessage_success_redirectsToWalletAndCallsService() throws Exception {
        mockMvc.perform(post("/wallet/messages/send")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("adminUsername", "admin")
                        .param("message", "I need help with my wallet."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attribute("successMessage", "Your message was sent to the admin."));

        verify(adminMailboxService).sendMessageToAdmin(eq("Plamen"), any(UserSendMessageRequest.class));
    }

    @Test
    void postSendMessage_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/wallet/messages/send")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("adminUsername", "")
                        .param("message", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/messages/send"));

        verify(adminMailboxService, never()).sendMessageToAdmin(any(), any());
    }

    @Test
    void postSendMessage_admin_redirectsToAdminSendPage() throws Exception {
        mockMvc.perform(post("/wallet/messages/send")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("adminUsername", "admin")
                        .param("message", "Hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/send"));

        verify(adminMailboxService, never()).sendMessageToAdmin(any(), any());
    }

    @Test
    void getMailbox_regularUser_returnsMessagesView() throws Exception {
        MailboxMessageViewDTO message = MailboxMessageViewDTO.builder()
                .id(UUID.randomUUID())
                .senderUsername("admin")
                .body("Welcome")
                .fromAdmin(true)
                .build();

        when(adminMailboxService.listMessagesForUser("Plamen")).thenReturn(List.of(message));
        when(adminMailboxService.countUnreadForUser("Plamen")).thenReturn(1L);

        mockMvc.perform(get("/wallet/messages")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet-messages"))
                .andExpect(model().attributeExists("messages"))
                .andExpect(model().attribute("unreadCount", 1L));

        verify(adminMailboxService).markAllUserMailboxRead("Plamen");
    }

    @Test
    void getMailbox_admin_redirectsToAdminInbox() throws Exception {
        mockMvc.perform(get("/wallet/messages")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/inbox"));

        verify(adminMailboxService, never()).listMessagesForUser(any());
    }

    @Test
    void getMessage_regularUser_returnsMessageView() throws Exception {
        UUID messageId = UUID.randomUUID();
        MailboxMessageViewDTO message = MailboxMessageViewDTO.builder()
                .id(messageId)
                .senderUsername("admin")
                .body("Please verify your email.")
                .fromAdmin(true)
                .build();

        when(adminMailboxService.viewMessageForUser("Plamen", messageId)).thenReturn(message);

        mockMvc.perform(get("/wallet/messages/{id}", messageId)
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet-message-view"))
                .andExpect(model().attribute("message", message));
    }

    @Test
    void getMessage_admin_redirectsToAdminInbox() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(get("/wallet/messages/{id}", messageId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/inbox"));

        verify(adminMailboxService, never()).viewMessageForUser(any(), any());
    }

    @Test
    void postDeleteMailbox_success_redirectsToMessages() throws Exception {
        mockMvc.perform(post("/wallet/messages/delete")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/messages"))
                .andExpect(flash().attribute("successMessage", "All messages were deleted."));

        verify(adminMailboxService).deleteUserMailbox("Plamen");
    }

    @Test
    void postDeleteMailbox_admin_redirectsToAdminInbox() throws Exception {
        mockMvc.perform(post("/wallet/messages/delete")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/inbox"));

        verify(adminMailboxService, never()).deleteUserMailbox(any());
    }
}
