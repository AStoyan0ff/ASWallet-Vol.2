package STARTER.DTOs;

import STARTER.Enums.SpendingCategory;
import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransferMoneyDTO {

    @NotBlank(message = "Receiver username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
            regexp = ValidationPatterns.USERNAME,
            message = "Username must start with a letter and contain only letters, numbers, or underscore"
    )
    private String receiverUsername;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = ValidationPatterns.MAX_TRANSACTION_AMOUNT, message = "Amount must not exceed 999999.99")
    private BigDecimal amount;

    @NotNull(message = "Spending category is required")
    private SpendingCategory spendingCategory;
}
