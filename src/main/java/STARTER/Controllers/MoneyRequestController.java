package STARTER.Controllers;

import STARTER.DTOs.MoneyRequestCreateDTO;
import STARTER.Services.Interface.MoneyRequestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/transactions/requests")
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    public MoneyRequestController(MoneyRequestService moneyRequestService) {
        this.moneyRequestService = moneyRequestService;
    }

    @GetMapping
    public String page(Model model, Principal principal) {
        String username = principal.getName();

        if (!model.containsAttribute("moneyRequestCreateDTO")) {
            model.addAttribute("moneyRequestCreateDTO", new MoneyRequestCreateDTO());
        }

        model.addAttribute("currentUsername", username);
        model.addAttribute("incomingRequests", moneyRequestService.listIncoming(username));
        model.addAttribute("outgoingRequests", moneyRequestService.listOutgoing(username));
        return "money-requests";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("moneyRequestCreateDTO") MoneyRequestCreateDTO dto,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("moneyRequestCreateDTO", dto);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.moneyRequestCreateDTO",
                    bindingResult
            );
            return "redirect:/transactions/requests";
        }

        moneyRequestService.create(principal.getName(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Money request sent.");
        return "redirect:/transactions/requests";
    }

    @PostMapping("/{id}/accept")
    public String accept(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        moneyRequestService.accept(id, principal.getName());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Money request accepted. Transfer submitted."
        );
        return "redirect:/transactions/requests";
    }

    @PostMapping("/{id}/decline")
    public String decline(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        moneyRequestService.decline(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Money request declined.");
        return "redirect:/transactions/requests";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable UUID id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        moneyRequestService.cancel(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Money request cancelled.");
        return "redirect:/transactions/requests";
    }

    @PostMapping("/clear")
    public String clearAll(Principal principal, RedirectAttributes redirectAttributes) {
        int deleted = moneyRequestService.deleteAllForUser(principal.getName());

        if (deleted == 0) {
            redirectAttributes.addFlashAttribute("successMessage", "No money requests to delete.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Deleted " + deleted + " money request(s)."
            );
        }

        return "redirect:/transactions/requests";
    }
}
