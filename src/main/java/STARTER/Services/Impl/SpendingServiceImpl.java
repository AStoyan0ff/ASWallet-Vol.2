package STARTER.Services.Impl;

import STARTER.CustomException.UserNotFoundException;
import STARTER.CustomException.WalletNotFoundException;
import STARTER.DTOs.SpendingCategorySliceDTO;
import STARTER.DTOs.SpendingInsightsViewDTO;
import STARTER.Enums.SpendingCategory;
import STARTER.Enums.SpendingPeriod;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Models.Transaction;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.SpendingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpendingServiceImpl implements SpendingService {

    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Sofia");
    private static final String OTHER_CATEGORY = "Other";

    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "Food", "#2e7d32",
            "Shopping", "#1565c0",
            "Bills", "#ef6c00",
            "Entertainment", "#6a1b9a",
            "Transport", "#00838f",
            OTHER_CATEGORY, "#5d4037"
    );

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public SpendingServiceImpl(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            WalletRepository walletRepository) {

        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SpendingInsightsViewDTO getInsights(String username, SpendingPeriod period) {

        SpendingPeriod safePeriod = period != null
            ? period
            : SpendingPeriod.THIS_WEEK;

        User user = userRepository.findByUsername(username) .orElseThrow(() ->
            new UserNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUser_Id(user.getId()) .orElseThrow(() ->
            new WalletNotFoundException("Wallet not found"));


        LocalDateTime fromInclusive = resolveFrom(safePeriod);
        LocalDateTime toExclusive = resolveToExclusive(safePeriod);

        List<Transaction> transactions = transactionRepository.findOutgoingSpending(
                wallet.getId(),
                List.of(TransactionType.TRANSFER, TransactionType.WITHDRAW),
                List.of(TransactionStatus.COMPLETED, TransactionStatus.PENDING, TransactionStatus.PENDING_RISK_REVIEW),
                fromInclusive,
                toExclusive
        );

        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (SpendingCategory category : SpendingCategory.values()) {
            totals.put(category.getLabel(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        totals.put(OTHER_CATEGORY, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        BigDecimal totalSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Transaction transaction : transactions) {

            BigDecimal amount = transaction.getAmount() != null
                    ? transaction.getAmount()
                    : BigDecimal.ZERO;

            String category = resolveCategory(transaction.getDescription());

            totals.merge(category, amount, BigDecimal::add);
            totalSpent = totalSpent.add(amount);
        }

        List<SpendingCategorySliceDTO> slices = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {

            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double percent = totalSpent.compareTo(BigDecimal.ZERO) == 0
                    ? 0
                    : entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalSpent, 1, RoundingMode.HALF_UP)
                    .doubleValue();

            slices.add(SpendingCategorySliceDTO.builder()
                    .category(entry.getKey())
                    .amount(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                    .percent(percent)
                    .color(CATEGORY_COLORS.getOrDefault(entry.getKey(), "#5d4037"))
                    .build());
        }

        slices.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        return SpendingInsightsViewDTO.builder()
                .period(safePeriod.name())
                .periodLabel(safePeriod.getLabel())
                .totalSpent(totalSpent.setScale(2, RoundingMode.HALF_UP))
                .transactionCount(transactions.size())
                .categories(slices)
                .empty(slices.isEmpty())
                .build();
    }

    private LocalDateTime resolveFrom(SpendingPeriod period) {
        LocalDate today = LocalDate.now(APP_ZONE);

        return switch (period) {

            case THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case THIS_MONTH -> today.withDayOfMonth(1).atStartOfDay();
            case ALL_TIME -> null;
        };
    }

    private LocalDateTime resolveToExclusive(SpendingPeriod period) {
        LocalDate today = LocalDate.now(APP_ZONE);

        return switch (period) {

            case THIS_WEEK -> today
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(1)
                    .atStartOfDay();

            case THIS_MONTH -> today
                    .withDayOfMonth(1)
                    .plusMonths(1)
                    .atStartOfDay();

            case ALL_TIME -> LocalDateTime.now(APP_ZONE);
        };
    }

    private String resolveCategory(String description) {

        if (description == null || description.isBlank()) {
            return OTHER_CATEGORY;
        }

        for (SpendingCategory category : SpendingCategory.values()) {
            String label = category.getLabel();

            if (description.equalsIgnoreCase(label)) {
                return label;
            }

            if (description.regionMatches(true, 0, label, 0, label.length()) &&
                description.length() > label.length()) {

                char next = description.charAt(label.length());

                if (next == ' ' ||
                    next == '(' ||
                    next == '-') {

                    return label;
                }
            }
        }

        return OTHER_CATEGORY;
    }
}
