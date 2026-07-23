package STARTER.Controllers;

import STARTER.DTOs.TransactionHistoryFilter;
import STARTER.DTOs.TransactionViewDTO;
import STARTER.Services.Interface.TransactionExportService;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class TransactionExportController {

    private final TransactionService transactionService;
    private final TransactionExportService transactionExportService;
    private final UserService userService;

    public TransactionExportController(
            TransactionService transactionService,
            TransactionExportService transactionExportService,
            UserService userService) {

        this.transactionService = transactionService;
        this.transactionExportService = transactionExportService;
        this.userService = userService;
    }

    @GetMapping("/wallet/export")
    public String exportPage(
            @ModelAttribute("filter") TransactionHistoryFilter filter,
            Model model,
            Principal principal) {

        List<TransactionViewDTO> transactions = loadFilteredTransactions(principal.getName(), filter);

        model.addAttribute("filter", filter);
        model.addAttribute("transactions", transactions);
        model.addAttribute("matchCount", transactions.size());

        return "transaction-export";
    }

    @GetMapping("/wallet/export/pdf")
    public void exportPdf(
            @ModelAttribute TransactionHistoryFilter filter,
            Principal principal,
            HttpServletResponse response) throws IOException {

        String username = principal.getName();
        List<TransactionViewDTO> transactions = loadFilteredTransactions(username, filter);

        transactionExportService.exportPdf(transactions, filter, username, response);
    }

    @ModelAttribute("filter")
    public TransactionHistoryFilter defaultFilter() {
        return new TransactionHistoryFilter();
    }

    private List<TransactionViewDTO> loadFilteredTransactions(String username, TransactionHistoryFilter filter) {
        UUID userId = userService.findByUsername(username).getId();
        return transactionService.getFilteredUserTransactions(userId, filter);
    }
}
