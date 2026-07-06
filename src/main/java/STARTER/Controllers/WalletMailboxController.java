package STARTER.Controllers;

import STARTER.DTOs.MailboxMessageViewDTO;
import STARTER.DTOs.UserSendMessageRequest;
import STARTER.Services.Interface.AdminMailboxService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// Advanced — user mailbox (/wallet/messages)
@Controller
public class WalletMailboxController {

    private final AdminMailboxService adminMailboxService;

    public WalletMailboxController(AdminMailboxService adminMailboxService) {
        this.adminMailboxService = adminMailboxService;
    }

    @GetMapping("/wallet/messages/send")
    public String sendMessagePage(Model model, Principal principal, Authentication authentication) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/messages/send";
        }

        if (!model.containsAttribute("userSendMessageRequest")) {
            model.addAttribute("userSendMessageRequest", new UserSendMessageRequest());
        }

        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("adminRecipients", adminMailboxService.listAdminRecipients());
        return "wallet-send-message";
    }

    @PostMapping("/wallet/messages/send")
    public String sendMessage(
            @Valid @ModelAttribute("userSendMessageRequest") UserSendMessageRequest request,
            BindingResult bindingResult,
            Principal principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            return "redirect:/admin/messages/send";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userSendMessageRequest", bindingResult);
            redirectAttributes.addFlashAttribute("userSendMessageRequest", request);
            return "redirect:/wallet/messages/send";
        }

        adminMailboxService.sendMessageToAdmin(principal.getName(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Your message was sent to the admin.");
        return "redirect:/wallet";
    }

    @GetMapping("/wallet/messages")
    public String mailbox(Model model, Principal principal, Authentication authentication) {

        if (isAdmin(authentication)) {
            return "redirect:/admin/messages/inbox";
        }

        adminMailboxService.markAllUserMailboxRead(principal.getName());
        List<MailboxMessageViewDTO> messages = adminMailboxService.listMessagesForUser(principal.getName());

        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", adminMailboxService.countUnreadForUser(principal.getName()));

        return "wallet-messages";
    }

    @GetMapping("/wallet/messages/{id}")
    public String viewMessage(@PathVariable UUID id, Model model, Principal principal, Authentication authentication) {

        if (isAdmin(authentication)) {
            return "redirect:/admin/messages/inbox";
        }

        MailboxMessageViewDTO message = adminMailboxService.viewMessageForUser(principal.getName(), id);
        model.addAttribute("message", message);

        return "wallet-message-view";
    }

    @PostMapping("/wallet/messages/delete")
    public String deleteMailbox(Principal principal, Authentication authentication, RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            return "redirect:/admin/messages/inbox";
        }

        adminMailboxService.deleteUserMailbox(principal.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "All messages were deleted.");

        return "redirect:/wallet/messages";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
