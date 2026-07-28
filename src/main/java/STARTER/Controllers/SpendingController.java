package STARTER.Controllers;

import STARTER.DTOs.SpendingCategorySliceDTO;
import STARTER.DTOs.SpendingInsightsViewDTO;
import STARTER.Enums.SpendingPeriod;
import STARTER.Services.Interface.SpendingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/transactions/spending")
public class SpendingController {

    private final SpendingService spendingService;

    public SpendingController(SpendingService spendingService) {
        this.spendingService = spendingService;
    }

    @GetMapping
    public String page(
            @RequestParam(value = "period", required = false) String periodParam,
            Model model,
            Principal principal,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
            .stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            return "redirect:/wallet";
        }

        SpendingPeriod period = SpendingPeriod.fromParam(periodParam);
        SpendingInsightsViewDTO insights = spendingService.getInsights(principal.getName(), period);
        List<SpendingCategorySliceDTO> categories = insights.getCategories();

        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("insights", insights);
        model.addAttribute("selectedPeriod", period.name());
        model.addAttribute("periods", SpendingPeriod.values());
        model.addAttribute("chartLabelsJson", toJsonStrings(
                categories.stream().map(SpendingCategorySliceDTO::getCategory).toList()
        ));

        model.addAttribute("chartAmountsJson", toJsonNumbers(
                categories.stream().map(SpendingCategorySliceDTO::getAmount).toList()
        ));

        model.addAttribute("chartColorsJson", toJsonStrings(
                categories.stream().map(SpendingCategorySliceDTO::getColor).toList()
        ));

        return "spending";
    }

    private String toJsonStrings(List<String> values) {

        return values.stream()
                .map(value -> "\"" + escapeJson(value) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toJsonNumbers(List<BigDecimal> values) {

        return values.stream()
                .map(value -> value == null
                    ? "0"
                    : value.toPlainString())
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
