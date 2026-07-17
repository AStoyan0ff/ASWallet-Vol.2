package STARTER.Controllers;

import STARTER.DTOs.AdminDashboardSummaryDTO;
import STARTER.DTOs.AdminUserViewDTO;
import STARTER.DTOs.LoginActivityViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import STARTER.Services.Interface.AdminDashboardService;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Services.Interface.AdminRiskReviewService;
import STARTER.Services.Interface.AdminService;
import STARTER.Services.Interface.LoginActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(AdminController.class)
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private AdminMailboxService adminMailboxService;

    @MockitoBean
    private LoginActivityService loginActivityService;

    @MockitoBean
    private AdminRiskReviewService adminRiskReviewService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    private UUID targetUserId;
    private AdminUserViewDTO manageableUser;

    @BeforeEach
    void setUp() {
        targetUserId = UUID.randomUUID();

        manageableUser = AdminUserViewDTO.builder()
                .id(targetUserId)
                .username("Plamen")
                .email("plamen@example.com")
                .balance(new BigDecimal("100.00"))
                .currency("EUR")
                .role(UserRole.USER)
                .roleDisplay("USER")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(adminRiskReviewService.countPendingReviews()).thenReturn(0L);
        when(adminDashboardService.getSummary()).thenReturn(
                AdminDashboardSummaryDTO.builder()
                        .pendingRiskReviews(0L)
                        .transfersToday(0L)
                        .unreadInbox(0L)
                        .activeUsers(0L)
                        .todayStatusLabels(List.of("Pending", "Risk hold", "Completed", "Failed", "Cancelled"))
                        .todayStatusCounts(List.of(0L, 0L, 0L, 0L, 0L))
                        .last7DaysLabels(List.of("a", "b", "c", "d", "e", "f", "g"))
                        .last7DaysCounts(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L))
                        .build()
        );
    }

    @Test
    void getDashboard_returnsAdminViewWithUsersAndContext() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(manageableUser));
        when(adminDashboardService.getSummary()).thenReturn(
                AdminDashboardSummaryDTO.builder()
                        .pendingRiskReviews(1L)
                        .transfersToday(4L)
                        .unreadInbox(3L)
                        .activeUsers(8L)
                        .todayStatusLabels(List.of("Pending", "Risk hold", "Completed", "Failed", "Cancelled"))
                        .todayStatusCounts(List.of(1L, 0L, 3L, 0L, 0L))
                        .last7DaysLabels(List.of("a", "b", "c", "d", "e", "f", "g"))
                        .last7DaysCounts(List.of(0L, 1L, 0L, 2L, 0L, 0L, 4L))
                        .build()
        );

        mockMvc.perform(get("/admin")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attribute("adminInboxUnreadCount", 3L))
                .andExpect(model().attribute("pendingRiskReviewCount", 1L))
                .andExpect(model().attribute("currentUsername", "admin"))
                .andExpect(model().attribute("primaryAdminUsername", "admin"))
                .andExpect(model().attribute("isPrimaryAdmin", true));
    }

    @Test
    void getLoginActivity_returnsViewWithActivities() throws Exception {
        LoginActivityViewDTO activity = LoginActivityViewDTO.builder()
                .username("Plamen")
                .ipAddress("192.168.1.10")
                .loggedInAt("2026-07-07 20:00:00")
                .build();

        when(loginActivityService.getLastLogins(10)).thenReturn(List.of(activity));

        mockMvc.perform(get("/admin/login-activity")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-activity"))
                .andExpect(model().attributeExists("loginActivities"))
                .andExpect(model().attribute("currentUsername", "admin"))
                .andExpect(model().attribute("isPrimaryAdmin", true));
    }

    @Test
    void clearLoginActivity_redirectsWithSuccessMessage() throws Exception {
        when(loginActivityService.clearAll()).thenReturn(5);

        mockMvc.perform(post("/admin/login-activity/clear")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login-activity"))
                .andExpect(flash().attribute("successMessage", "Cleared 5 login activity record(s)."));

        verify(loginActivityService).clearAll();
    }

    @Test
    void clearLoginActivity_whenEmpty_redirectsWithNoRecordsMessage() throws Exception {
        when(loginActivityService.clearAll()).thenReturn(0);

        mockMvc.perform(post("/admin/login-activity/clear")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login-activity"))
                .andExpect(flash().attribute("successMessage", "No login activity to clear."));
    }

    @Test
    void getManageUser_returnsViewWithUser() throws Exception {
        when(adminService.getManageableUser("admin", targetUserId)).thenReturn(manageableUser);

        mockMvc.perform(get("/admin/users/{id}/manage", targetUserId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-manage"))
                .andExpect(model().attribute("user", manageableUser))
                .andExpect(model().attribute("currentUsername", "admin"));
    }

    @Test
    void postDeleteUser_success_redirectsToAdmin() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/delete", targetUserId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("successMessage", "User removed successfully."));

        verify(adminService).deleteUser("admin", targetUserId);
    }

    @Test
    void postUpdateStatus_success_redirectsToManagePage() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/status", targetUserId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("status", AccountStatus.INACTIVE.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + targetUserId + "/manage"))
                .andExpect(flash().attribute("successMessage", "Account status updated successfully."));

        verify(adminService).updateAccountStatus("admin", targetUserId, AccountStatus.INACTIVE);
    }

    @Test
    void postUpdateRole_promoteToAdmin_redirectsToAdminWithMessage() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/role", targetUserId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("role", UserRole.ADMIN.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "User promoted to support admin successfully. They must log in again to access the admin panel."
                ));

        verify(adminService).updateUserRole("admin", targetUserId, UserRole.ADMIN);
    }

    @Test
    void postUpdateRole_demoteToUser_redirectsToManagePage() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/role", targetUserId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("role", UserRole.USER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + targetUserId + "/manage"))
                .andExpect(flash().attribute("successMessage", "User role updated successfully."));

        verify(adminService).updateUserRole(eq("admin"), eq(targetUserId), eq(UserRole.USER));
    }
}
