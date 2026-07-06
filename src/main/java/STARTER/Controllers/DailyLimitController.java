package STARTER.Controllers;

import STARTER.Services.Interface.UserProfileDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
public class DailyLimitController {

    private final UserProfileDetailsService userProfileDetailsService;
    private final BigDecimal withdrawLimitMin;
    private final BigDecimal withdrawLimitMax;
    private final int withdrawLimitStep;

    public DailyLimitController(
            UserProfileDetailsService userProfileDetailsService,
            @Value("${app.withdraw.daily-limit.min:50}") BigDecimal withdrawLimitMin,
            @Value("${app.withdraw.daily-limit.max:500}") BigDecimal withdrawLimitMax,
            @Value("${app.withdraw.daily-limit.step:50}") int withdrawLimitStep
    ) {
        this.userProfileDetailsService = userProfileDetailsService;
        this.withdrawLimitMin = withdrawLimitMin;
        this.withdrawLimitMax = withdrawLimitMax;
        this.withdrawLimitStep = withdrawLimitStep;
    }

    @GetMapping("/wallet/daily-limit/edit")
    public String editDailyLimitPage(Model model, Principal principal, Authentication authentication) {
        if (isAdmin(authentication)) {
            return "redirect:/wallet/settings";
        }

        if (!model.containsAttribute("dailyLimitEditRequest")) {
            model.addAttribute(
                    "dailyLimitEditRequest",
                    userProfileDetailsService.buildDailyLimitEditRequest(principal.getName())
            );
        }

        addLimitAttributes(model);
        return "daily-limit-edit";
    }

    @PostMapping("/wallet/daily-limit/edit")
    public String saveDailyLimit(
            @RequestParam BigDecimal dailyWithdrawLimit,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        userProfileDetailsService.updateDailyLimit(principal.getName(), dailyWithdrawLimit);
        redirectAttributes.addFlashAttribute("successMessage", "Daily limit updated successfully.");
        return "redirect:/wallet/settings";
    }

    private void addLimitAttributes(Model model) {
        model.addAttribute("withdrawLimitMin", withdrawLimitMin);
        model.addAttribute("withdrawLimitMax", withdrawLimitMax);
        model.addAttribute("withdrawLimitStep", withdrawLimitStep);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
