package STARTER.Controllers;

import STARTER.DTOs.ForgotPasswordRequest;
import STARTER.DTOs.ResetPasswordRequest;
import STARTER.Services.Interface.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private static final String RESET_EMAIL_SENT_MESSAGE =
            "If an account with that email exists, we have sent password reset instructions.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {

        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("forgotPasswordRequest", request);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.forgotPasswordRequest",
                    bindingResult
            );
            return "redirect:/forgot-password";
        }

        passwordResetService.requestPasswordReset(request.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", RESET_EMAIL_SENT_MESSAGE);
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!passwordResetService.isTokenValid(token)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This reset link is invalid or has expired."
            );
            return "redirect:/forgot-password";
        }

        if (!model.containsAttribute("resetPasswordRequest")) {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token(token)
                    .build();

            model.addAttribute("resetPasswordRequest", request);
        }

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute("resetPasswordRequest", request);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.resetPasswordRequest",
                    bindingResult
            );
            return "redirect:/reset-password?token=" + request.getToken();
        }

        passwordResetService.resetPassword(request.getToken(), request);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Password reset successfully. Please log in with your new password."
        );
        return "redirect:/login";
    }
}
