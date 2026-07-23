package STARTER.Controllers;

import STARTER.DTOs.TransactionViewDTO;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
public class TransactionHistoryController {

    static final int PAGE_SIZE = 5;

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionHistoryController(
            TransactionService transactionService,
            UserService userService) {

        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/transactions/history")
    public String history(
            @RequestParam(defaultValue = "0") int page,
            Model model,
            Principal principal) {

        UUID userId = userService.findByUsername(principal.getName()).getId();

        Page<TransactionViewDTO> transactionPage =
                transactionService.getUserTransactionsPage(userId, page, PAGE_SIZE);

        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", transactionPage.getNumber());
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalItems", transactionPage.getTotalElements());
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("hasPrevious", transactionPage.hasPrevious());
        model.addAttribute("hasNext", transactionPage.hasNext());
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("hasPendingTransfers", transactionService.hasPendingTransfers(userId));

        return "transaction-history";
    }

    @PostMapping("/transactions/{id}/cancel")
    public String cancelPendingTransfer(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        UUID userId = userService.findByUsername(principal.getName()).getId();
        transactionService.cancelPendingTransfer(id, userId);

        redirectAttributes.addFlashAttribute("successMessage",
                                            "Pending transfer cancelled. Funds returned to your wallet.");

        return "redirect:/transactions/history?page=" + Math.max(page, 0);
    }

    @PostMapping("/transactions/history/clear")
    public String clearHistory(Principal principal, RedirectAttributes redirectAttributes) {
        UUID userId = userService.findByUsername(principal.getName()).getId();

        transactionService.clearUserTransactionHistory(userId);

        redirectAttributes.addFlashAttribute("successMessage",
                                            "Transaction history cleared successfully.");

        return "redirect:/transactions/history";
    }
}
