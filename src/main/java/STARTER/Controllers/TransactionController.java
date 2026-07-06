package STARTER.Controllers;

import STARTER.DTOs.DepositMoneyDTO;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.DTOs.WalletViewDTO;
import STARTER.DTOs.WithdrawMoneyDTO;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import STARTER.Services.Interface.WalletService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
public class TransactionController {

    private static final String SESSION_PENDING_TRANSFER = "pendingTransferMoneyDTO";

    private final TransactionService transactionService;
    private final UserService userService;
    private final BankCardService bankCardService;
    private final WalletService walletService;
    private final WithdrawDailyLimitService withdrawDailyLimitService;

    public TransactionController(
            TransactionService transactionService,
            UserService userService,
            BankCardService bankCardService,
            WalletService walletService,
            WithdrawDailyLimitService withdrawDailyLimitService
    ) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.bankCardService = bankCardService;
        this.walletService = walletService;
        this.withdrawDailyLimitService = withdrawDailyLimitService;
    }

    @ModelAttribute("transferMoneyDTO")
    public TransferMoneyDTO transferMoney(TransferMoneyDTO transferMoneyDTO) {
        return transferMoneyDTO != null
                ? transferMoneyDTO
                : new TransferMoneyDTO();

    }

    @ModelAttribute("depositMoneyDTO")
    public DepositMoneyDTO depositMoney(DepositMoneyDTO depositMoneyDTO) {
        return depositMoneyDTO != null
                ? depositMoneyDTO
                : new DepositMoneyDTO();
    }

    @ModelAttribute("withdrawMoneyDTO")
    public WithdrawMoneyDTO withdrawMoney(WithdrawMoneyDTO withdrawMoneyDTO) {
        return withdrawMoneyDTO != null
                ? withdrawMoneyDTO
                : new WithdrawMoneyDTO();
    }

    @GetMapping("/transactions/transfer")
    public String transfer(HttpSession session, Model model) {
        Object pending = session.getAttribute(SESSION_PENDING_TRANSFER);

        if (pending instanceof TransferMoneyDTO dto) {
            model.addAttribute("transferMoneyDTO", dto);
        }

        return "transfer";
    }

    @PostMapping("/transactions/transfer")
    public String transferReview(@Valid TransferMoneyDTO transferMoneyDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("transferMoneyDTO", transferMoneyDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transferMoneyDTO", bindingResult);

            return "redirect:/transactions/transfer";
        }

        session.setAttribute(SESSION_PENDING_TRANSFER, transferMoneyDTO);
        return "redirect:/transactions/transfer/confirm";
    }

    @GetMapping("/transactions/transfer/confirm")
    public String transferConfirmPage(HttpSession session, Model model, Principal principal) {

        Object pending = session.getAttribute(SESSION_PENDING_TRANSFER);
        if (!(pending instanceof TransferMoneyDTO transferMoneyDTO)) {
            return "redirect:/transactions/transfer";
        }

        WalletViewDTO wallet = walletService.getWalletByUsername(principal.getName());

        model.addAttribute("transferMoneyDTO", transferMoneyDTO);
        model.addAttribute("wallet", wallet);
        model.addAttribute("balanceAfter", wallet.getBalance().subtract(transferMoneyDTO.getAmount()));
        return "transfer-confirm";
    }

    @PostMapping("/transactions/transfer/confirm")
    public String transferExecute(HttpSession session,
                                  RedirectAttributes redirectAttributes,
                                  Principal principal) {

        Object pending = session.getAttribute(SESSION_PENDING_TRANSFER);

        if (!(pending instanceof TransferMoneyDTO transferMoneyDTO)) {

            redirectAttributes.addFlashAttribute("errorMessage", "Transfer session expired. Please try again.");
            return "redirect:/transactions/transfer";
        }

        UUID senderUserId = userService
                .findByUsername(principal.getName())
                .getId();

        transactionService.transfer(senderUserId, transferMoneyDTO);
        session.removeAttribute(SESSION_PENDING_TRANSFER);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Transfer submitted. It will be processed shortly — check Transaction History for status."
        );
        return "redirect:/transactions/history";
    }

    @GetMapping("/transactions/deposit")
    public ModelAndView deposit(Principal principal) {

        ModelAndView mv = new ModelAndView("deposit");
        mv.addObject("wallet", walletService.getWalletByUsername(principal.getName()));

        bankCardService.getBankCardByUsername(principal.getName())
                .ifPresent(card -> mv.addObject("savedBankCard", card));
        return mv;
    }

    @PostMapping("/transactions/deposit")
    public String depositConfirm(@Valid DepositMoneyDTO depositMoneyDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Principal principal) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("depositMoneyDTO", depositMoneyDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.depositMoneyDTO", bindingResult);

            return "redirect:/transactions/deposit";
        }

        if (bankCardService.getBankCardByUsername(principal.getName()).isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please add bank details before depositing."
            );
            return "redirect:/transactions/deposit";
        }

        UUID userId = userService
            .findByUsername(principal.getName())
            .getId();

        transactionService.deposit(userId, depositMoneyDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Deposit completed successfully.");
        return "redirect:/wallet";
    }

    @GetMapping("/transactions/withdraw")
    public String withdraw(Model model, Principal principal) {
        bankCardService.getBankCardByUsername(principal.getName())
                .ifPresent(card -> model.addAttribute("savedBankCard", card));
        model.addAttribute("withdrawDailyLimit", withdrawDailyLimitService.getViewForUsername(principal.getName()));
        return "withdraw";
    }

    @PostMapping("/transactions/withdraw")
    public String withdrawConfirm(@Valid WithdrawMoneyDTO withdrawMoneyDTO,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Principal principal) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("withdrawMoneyDTO", withdrawMoneyDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.withdrawMoneyDTO", bindingResult);

            return "redirect:/transactions/withdraw";
        }

        if (bankCardService.getBankCardByUsername(principal.getName()).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please add bank details before withdrawing."
            );
            return "redirect:/transactions/withdraw";
        }

        UUID userId = userService
            .findByUsername(principal.getName())
            .getId();

        transactionService.withdraw(userId, withdrawMoneyDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal completed successfully.");

        return "redirect:/wallet";
    }
}
