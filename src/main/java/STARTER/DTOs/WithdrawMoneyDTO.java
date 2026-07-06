package STARTER.DTOs;

import STARTER.Enums.SpendingCategory;
import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawMoneyDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = ValidationPatterns.MAX_TRANSACTION_AMOUNT, message = "Amount must not exceed 999999.99")
    private BigDecimal amount;

    @NotNull(message = "Spending category is required")
    private SpendingCategory spendingCategory;
}
