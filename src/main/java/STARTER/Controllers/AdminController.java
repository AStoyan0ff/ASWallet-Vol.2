package STARTER.Controllers;

import STARTER.DTOs.AdminUserViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Services.Interface.AdminRiskReviewService;
import STARTER.Services.Interface.AdminService;
import STARTER.Services.Interface.LoginActivityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminMailboxService adminMailboxService;
    private final LoginActivityService loginActivityService;
    private final AdminRiskReviewService adminRiskReviewService;

    @Value("${app.admin.username:admin}")
    private String primaryAdminUsername;

    public AdminController(

            AdminService adminService,
            AdminMailboxService adminMailboxService,
            LoginActivityService loginActivityService,
            AdminRiskReviewService adminRiskReviewService

    ) {

        this.adminService = adminService;
        this.adminMailboxService = adminMailboxService;
        this.loginActivityService = loginActivityService;
        this.adminRiskReviewService = adminRiskReviewService;
    }

    @GetMapping
    public String dashboard(Model model, Principal principal) {

        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("adminInboxUnreadCount", adminMailboxService.countUnreadForAdminInbox());
        model.addAttribute("pendingRiskReviewCount", adminRiskReviewService.countPendingReviews());
        addAdminContext(model, principal);

        return "admin";
    }

    // Advanced — last successful logins for admins
    @GetMapping("/login-activity")
    public String loginActivityPage(Model model, Principal principal) {

        model.addAttribute("loginActivities", loginActivityService.getLastLogins(10));
        addAdminContext(model, principal);

        return "admin-login-activity";
    }

    @PostMapping("/login-activity/clear")
    public String clearLoginActivity(Principal principal, RedirectAttributes redirectAttributes) {
        int deleted = loginActivityService.clearAll();

        if (deleted == 0) {
            redirectAttributes.addFlashAttribute("successMessage", "No login activity to clear.");

        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cleared " + deleted + " login activity record(s)."
            );
        }

        return "redirect:/admin/login-activity";
    }

    // Advanced — manage user status and role
    @GetMapping("/users/{id}/manage")
    public String manageUserPage(

            @PathVariable UUID id,
            Model model,
            Principal principal) {

        AdminUserViewDTO user = adminService.getManageableUser(principal.getName(), id);

        model.addAttribute("user", user);
        addAdminContext(model, principal);
        return "admin-user-manage";
    }

    private void addAdminContext(Model model, Principal principal) {
        String currentUsername = principal.getName();

        model.addAttribute("currentUsername", currentUsername);
        model.addAttribute("primaryAdminUsername", primaryAdminUsername);
        model.addAttribute("isPrimaryAdmin", primaryAdminUsername.trim().equals(currentUsername));
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        adminService.deleteUser(principal.getName(), id);

        redirectAttributes.addFlashAttribute("successMessage", "User removed successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/status")
    public String updateAccountStatus(
            @PathVariable UUID id,
            @RequestParam AccountStatus status,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        adminService.updateAccountStatus(principal.getName(), id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Account status updated successfully.");
        return "redirect:/admin/users/" + id + "/manage";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(
            @PathVariable UUID id,
            @RequestParam UserRole role,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        adminService.updateUserRole(principal.getName(), id, role);

        if (role == UserRole.ADMIN) {

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "User promoted to support admin successfully. They must log in again to access the admin panel.");

            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully.");
        return "redirect:/admin/users/" + id + "/manage";
    }
}

