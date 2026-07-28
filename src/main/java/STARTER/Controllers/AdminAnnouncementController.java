package STARTER.Controllers;

import STARTER.DTOs.SystemAnnouncementFormDTO;
import STARTER.DTOs.SystemAnnouncementViewDTO;
import STARTER.Services.Interface.SystemAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/announcement")
public class AdminAnnouncementController {

    private final SystemAnnouncementService announcementService;

    public AdminAnnouncementController(SystemAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public String page(Model model, Principal principal) {
        SystemAnnouncementViewDTO latest = announcementService.getLatestAnnouncement().orElse(null);

        if (!model.containsAttribute("announcementForm")) {

            SystemAnnouncementFormDTO form = new SystemAnnouncementFormDTO();

            if (latest != null && latest.getMessage() != null) {
                form.setMessage(latest.getMessage());
            }

            model.addAttribute("announcementForm", form);
        }

        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("latestAnnouncement", latest);

        return "admin-announcement";
    }

    @PostMapping("/publish")
    public String publish(
            @Valid @ModelAttribute("announcementForm") SystemAnnouncementFormDTO form,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {

            model.addAttribute("currentUsername", principal.getName());
            model.addAttribute("latestAnnouncement", announcementService.getLatestAnnouncement().orElse(null));
            return "admin-announcement";
        }

        announcementService.publish(form.getMessage(), principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Announcement published. Users will see it on Wallet.");
        return "redirect:/admin/announcement";
    }

    @PostMapping("/deactivate")
    public String deactivate(Principal principal, RedirectAttributes redirectAttributes) {

        announcementService.deactivate(principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Announcement turned off.");
        return "redirect:/admin/announcement";
    }

    @PostMapping("/clear")
    public String clear(RedirectAttributes redirectAttributes) {
        int deleted = announcementService.clearAll();

        if (deleted == 0) {
            redirectAttributes.addFlashAttribute("successMessage", "No announcements to clear.");

        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Cleared " + deleted + " announcement record(s).");

        }

        return "redirect:/admin/announcement";
    }
}
