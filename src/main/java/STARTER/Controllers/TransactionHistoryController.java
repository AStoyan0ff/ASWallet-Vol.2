package STARTER.Controllers;

import STARTER.DTOs.TransactionViewDTO;
import STARTER.Enums.TransactionStatus;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// Advanced — transaction history
@Controller
public class TransactionHistoryController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionHistoryController(
            TransactionService transactionService,
            UserService userService) {

        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/transactions/history")
    public String history(Model model, Principal principal) {
        UUID userId = userService.findByUsername(principal.getName()).getId();
        List<TransactionViewDTO> transactions = transactionService.getUserTransactions(userId);

        model.addAttribute("transactions", transactions.stream().limit(5).toList());
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("hasPendingTransfers", transactions.stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.PENDING));

        return "transaction-history";
    }

    @PostMapping("/transactions/{id}/cancel")
    public String cancelPendingTransfer(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        UUID userId = userService.findByUsername(principal.getName()).getId();
        transactionService.cancelPendingTransfer(id, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Pending transfer cancelled. Funds returned to your wallet.");

        return "redirect:/transactions/history";
    }

    @PostMapping("/transactions/history/clear")
    public String clearHistory(Principal principal, RedirectAttributes redirectAttributes) {
        UUID userId = userService.findByUsername(principal.getName()).getId();

        transactionService.clearUserTransactionHistory(userId);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction history cleared successfully.");

        return "redirect:/transactions/history";
    }
}
