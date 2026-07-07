package STARTER.Controllers;

import STARTER.DTOs.AdminSendMessageRequest;
import STARTER.DTOs.MailboxMessageViewDTO;
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

@WebMvcTest(AdminMailboxController.class)
class AdminMailboxControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMailboxService adminMailboxService;

    @Test
    void getSendMessagePage_returnsViewWithForm() throws Exception {
        mockMvc.perform(get("/admin/messages/send")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-send-message"))
                .andExpect(model().attributeExists("adminSendMessageRequest"))
                .andExpect(model().attribute("currentUsername", "admin"));
    }

    @Test
    void postSendMessage_success_redirectsToWalletAndCallsService() throws Exception {
        mockMvc.perform(post("/admin/messages/send")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "Plamen")
                        .param("message", "Your transfer is ready."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attribute("successMessage", "Message sent to Plamen successfully."));

        verify(adminMailboxService).sendMessageToUser(eq("admin"), any(AdminSendMessageRequest.class));
    }

    @Test
    void postSendMessage_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/messages/send")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "ab")
                        .param("message", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/send"));

        verify(adminMailboxService, never()).sendMessageToUser(any(), any());
    }

    @Test
    void getAdminInbox_returnsViewWithReplies() throws Exception {
        MailboxMessageViewDTO reply = MailboxMessageViewDTO.builder()
                .id(UUID.randomUUID())
                .senderUsername("Plamen")
                .body("Need help with my wallet.")
                .preview("Need help...")
                .fromAdmin(false)
                .build();

        when(adminMailboxService.listAdminInbox()).thenReturn(List.of(reply));
        when(adminMailboxService.countUnreadForAdminInbox()).thenReturn(0L);

        mockMvc.perform(get("/admin/messages/inbox")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-message-inbox"))
                .andExpect(model().attributeExists("replies"))
                .andExpect(model().attribute("unreadCount", 0L));

        verify(adminMailboxService).markAllAdminInboxRead("admin");
    }

    @Test
    void postDeleteInboxMessages_success_redirectsToInbox() throws Exception {
        mockMvc.perform(post("/admin/messages/inbox/delete")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages/inbox"))
                .andExpect(flash().attribute("successMessage", "All message threads were deleted."));

        verify(adminMailboxService).deleteAllInboxMessageThreads("admin");
    }

    @Test
    void getUserThread_returnsViewWithThread() throws Exception {
        MailboxMessageViewDTO message = MailboxMessageViewDTO.builder()
                .id(UUID.randomUUID())
                .senderUsername("Plamen")
                .body("Hello admin")
                .fromAdmin(false)
                .build();

        when(adminMailboxService.listThreadForUser("Plamen")).thenReturn(List.of(message));

        mockMvc.perform(get("/admin/messages/users/{username}/thread", "Plamen")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-message-thread"))
                .andExpect(model().attribute("threadUsername", "Plamen"))
                .andExpect(model().attributeExists("thread"));

        verify(adminMailboxService).markUserThreadRead("admin", "Plamen");
    }
}
