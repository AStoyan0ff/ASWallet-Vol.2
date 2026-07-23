package STARTER.Controllers;

import STARTER.DTOs.AdminSendMessageRequest;
import STARTER.DTOs.MailboxMessageViewDTO;
import STARTER.Services.Interface.AdminMailboxService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/messages")
public class AdminMailboxController {

    private final AdminMailboxService adminMailboxService;

    public AdminMailboxController(AdminMailboxService adminMailboxService) {
        this.adminMailboxService = adminMailboxService;
    }

    @GetMapping("/send")
    public String sendMessagePage(Model model, Principal principal) {
        if (!model.containsAttribute("adminSendMessageRequest")) {
            model.addAttribute("adminSendMessageRequest", new AdminSendMessageRequest());
        }

        model.addAttribute("currentUsername", principal.getName());
        return "admin-send-message";
    }

    @PostMapping("/send")
    public String sendMessage(
            @Valid @ModelAttribute("adminSendMessageRequest") AdminSendMessageRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminSendMessageRequest", bindingResult);
            redirectAttributes.addFlashAttribute("adminSendMessageRequest", request);
            return "redirect:/admin/messages/send";
        }

        adminMailboxService.sendMessageToUser(principal.getName(), request);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Message sent to " + request.getUsername().trim() + " successfully."
        );

        return "redirect:/wallet";
    }

    @GetMapping("/inbox")
    public String adminInbox(Model model, Principal principal) {
        adminMailboxService.markAllAdminInboxRead(principal.getName());

        List<MailboxMessageViewDTO> replies = adminMailboxService.listAdminInbox();

        model.addAttribute("replies", replies);
        model.addAttribute("unreadCount", adminMailboxService.countUnreadForAdminInbox());

        return "admin-message-inbox";
    }

    @PostMapping("/inbox/delete")
    public String deleteInboxMessages(Principal principal, RedirectAttributes redirectAttributes) {

        adminMailboxService.deleteAllInboxMessageThreads(principal.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                                            "All message threads were deleted.");
        return "redirect:/admin/messages/inbox";
    }

    @GetMapping("/users/{username}/thread")
    public String userThread(
            @PathVariable String username,
            Model model,
            Principal principal) {

        List<MailboxMessageViewDTO> thread = adminMailboxService.listThreadForUser(username);
        adminMailboxService.markUserThreadRead(principal.getName(), username);

        model.addAttribute("threadUsername", username);
        model.addAttribute("thread", thread);

        return "admin-message-thread";
    }
}
