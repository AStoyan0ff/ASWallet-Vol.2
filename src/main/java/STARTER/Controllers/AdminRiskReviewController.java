package STARTER.Controllers;

import STARTER.Services.Interface.AdminRiskReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/admin/risk-reviews")
public class AdminRiskReviewController {

    private final AdminRiskReviewService adminRiskReviewService;

    public AdminRiskReviewController(AdminRiskReviewService adminRiskReviewService) {
        this.adminRiskReviewService = adminRiskReviewService;
    }

    @GetMapping
    public String listPendingReviews(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("assessments", adminRiskReviewService.listRiskReviews());
        } catch (RuntimeException ex) {
            model.addAttribute("assessments", java.util.List.of());
            model.addAttribute("errorMessage", ex.getMessage());
        }

        model.addAttribute("currentUsername", principal.getName());
        return "admin-risk-reviews";
    }

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        return review(id, principal, redirectAttributes, true);
    }

    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        return review(id, principal, redirectAttributes, false);
    }

    @PostMapping("/clear")
    public String clearAll(
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            int deleted = adminRiskReviewService.deleteAllRiskReviews();

            if (deleted == 0) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "No risk reviews to delete."
                );

            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Deleted " + deleted + " risk review(s). Pending transfers were cancelled and funds returned to senders."
                );
            }

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/risk-reviews";
    }

    private String review(
            UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes,
            boolean approve) {

        try {

            if (approve) {
                adminRiskReviewService.approve(id, principal.getName());
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Risk assessment approved."
                );

            } else {
                adminRiskReviewService.reject(id, principal.getName());
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Risk assessment rejected."
                );
            }

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/risk-reviews";
    }
}
