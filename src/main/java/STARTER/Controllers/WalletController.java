package STARTER.Controllers;

import STARTER.DTOs.BankCardRequest;
import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.DeleteAccountRequest;
import STARTER.DTOs.WalletSettingsRequest;
import STARTER.DTOs.WalletViewDTO;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Services.Interface.AdminRiskReviewService;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Services.Interface.UserService;
import STARTER.Services.Interface.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final BankCardService bankCardService;
    private final UserProfileDetailsService userProfileDetailsService;
    private final AdminMailboxService adminMailboxService;
    private final AdminRiskReviewService adminRiskReviewService;

    public WalletController(
            WalletService walletService,
            UserService userService,
            BankCardService bankCardService,
            UserProfileDetailsService userProfileDetailsService,
            AdminMailboxService adminMailboxService,
            AdminRiskReviewService adminRiskReviewService
    ) {
        this.walletService = walletService;
        this.userService = userService;
        this.bankCardService = bankCardService;
        this.userProfileDetailsService = userProfileDetailsService;
        this.adminMailboxService = adminMailboxService;
        this.adminRiskReviewService = adminRiskReviewService;
    }
//      addAttribute() -> Добавя данни към текущия request

    @GetMapping("/wallet")
    public String wallet(Model model, Principal principal, Authentication authentication) {
        WalletViewDTO wallet = walletService.getWalletByUsername(principal.getName());

        model.addAttribute("wallet", wallet);
        model.addAttribute("balanceHidden", userProfileDetailsService.isBalanceHidden(principal.getName()));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            model.addAttribute("adminInboxUnreadCount", adminMailboxService.countUnreadForAdminInbox());
            model.addAttribute("pendingRiskReviewCount", adminRiskReviewService.countPendingReviews());
        } else {
            model.addAttribute("unreadMessageCount", adminMailboxService.countUnreadForUser(principal.getName()));
        }

        bankCardService.getBankCardByUsername(principal.getName())
                .ifPresent(card -> model.addAttribute("savedBankCard", card));

        return "wallet";
    }

    @GetMapping("/wallet/bank-details")
    public String bankDetailsPage(Model model, Principal principal) {

        model.addAttribute("user", userService.findByUsername(principal.getName()));
        bankCardService.getBankCardByUsername(principal.getName())
                .ifPresent(card -> model.addAttribute("savedBankCard", card));

        if (!model.containsAttribute("bankCardRequest")) {
            model.addAttribute("bankCardRequest", new BankCardRequest());
        }

        return "bank-details";
    }

    @PostMapping("/wallet/bank-details")
    public String saveBankCard(
            @Valid @ModelAttribute("bankCardRequest") BankCardRequest bankCardRequest,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("bankCardRequest", bankCardRequest);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.bankCardRequest",
                    bindingResult
            );
            return "redirect:/wallet/bank-details";
        }

        if (bankCardService.getBankCardByUsername(principal.getName()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bank card is already registered.");
            return "redirect:/wallet/bank-details";
        }

        boolean welcomeBonusGranted = bankCardService.saveBankCard(principal.getName(), bankCardRequest);

        if (welcomeBonusGranted) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Bank card saved. €50.00 welcome bonus added to your wallet."
            );

        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Bank card saved successfully.");
        }

        return "redirect:/wallet/bank-details";
    }

    @GetMapping("/wallet/change-password")
    public String changePasswordPage(Model model) {

        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }

        return "change-password";
    }
//      addFlashAttribute() -> Запазва данните временно за следващия request без да ги показва в URL

    @PostMapping("/wallet/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("changePasswordRequest", request);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.changePasswordRequest",
                    bindingResult
            );

            return "redirect:/wallet/change-password";
        }

        userService.changePassword(principal.getName(), request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Password changed successfully"
        );

        return "redirect:/wallet/change-password";
    }

    // Advanced — wallet settings (/wallet/settings)
    @GetMapping("/wallet/settings")
    public String settingsPage(Model model, Principal principal, Authentication authentication) {

        if (!model.containsAttribute("walletSettingsRequest")) {

            model.addAttribute("walletSettingsRequest",
                    userProfileDetailsService.buildWalletSettingsRequest(principal.getName())
            );
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("isAdmin", isAdmin);
        return "settings";
    }

    @PostMapping("/wallet/settings")
    public Object saveSettings(
            @RequestParam(defaultValue = "false") boolean balanceHidden,
            @RequestParam(defaultValue = "false") boolean emailOnDeposit,
            @RequestParam(defaultValue = "false") boolean emailOnWithdraw,
            @RequestParam(defaultValue = "false") boolean emailOnTransfer,
            Principal principal,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith
    ) {
        WalletSettingsRequest walletSettingsRequest = new WalletSettingsRequest();

        walletSettingsRequest.setBalanceHidden(balanceHidden);
        walletSettingsRequest.setEmailOnDeposit(emailOnDeposit);
        walletSettingsRequest.setEmailOnWithdraw(emailOnWithdraw);
        walletSettingsRequest.setEmailOnTransfer(emailOnTransfer);

        userProfileDetailsService.updateWalletSettings(principal.getName(), walletSettingsRequest);

        if ("XMLHttpRequest".equals(requestedWith)) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Settings saved successfully.",
                    "balanceHidden", balanceHidden,
                    "emailOnDeposit", emailOnDeposit,
                    "emailOnWithdraw", emailOnWithdraw,
                    "emailOnTransfer", emailOnTransfer
            ));
        }

        redirectAttributes.addFlashAttribute("successMessage", "Settings saved successfully.");
        return "redirect:/wallet/settings";
    }

    @GetMapping("/wallet/delete-account")
    public String deleteAccountPage(Model model) {

        if (!model.containsAttribute("deleteAccountRequest")) {
            model.addAttribute("deleteAccountRequest", new DeleteAccountRequest());
        }
        return "delete-account";
    }

    @PostMapping("/wallet/delete-account")
    public String deleteAccount(
            @Valid @ModelAttribute("deleteAccountRequest") DeleteAccountRequest request,
            BindingResult bindingResult,
            Principal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute("deleteAccountRequest", request);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.deleteAccountRequest",
                    bindingResult);

            return "redirect:/wallet/delete-account";
        }

        userService.deleteAccount(principal.getName(), request);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, null);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your account has been deleted successfully.");

        return "redirect:/account-deleted";
    }

    @GetMapping("/account-deleted")
    public String accountDeleted() {
        return "account-deleted";
    }
}
