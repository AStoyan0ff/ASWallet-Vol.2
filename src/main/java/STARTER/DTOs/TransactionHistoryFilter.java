package STARTER.DTOs;

import STARTER.Enums.SpendingCategory;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

// Advanced: shared filters for history and exports
@Getter
@Setter
@NoArgsConstructor
public class TransactionHistoryFilter {

    private TransactionType type;
    private TransactionStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private SpendingCategory spendingCategory;
    private BigDecimal amount;

    public boolean hasActiveFilters() {
        return type != null
                || status != null
                || dateFrom != null
                || dateTo != null
                || spendingCategory != null
                || amount != null;
    }
}
