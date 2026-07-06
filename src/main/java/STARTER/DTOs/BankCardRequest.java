package STARTER.DTOs;

import STARTER.Utils.CardValidationUtils;
import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankCardRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = ValidationPatterns.CARD_NUMBER, message = "Card number must be exactly 16 digits")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required")
    @Size(min = 2, max = 80, message = "Cardholder name must be between 2 and 80 characters")
    @Pattern(
            regexp = ValidationPatterns.CARDHOLDER_NAME,
            message = "Cardholder name must start with a letter and contain only letters, spaces, hyphen, or apostrophe"
    )
    private String cardholderName;

    @NotBlank(message = "Expiry month is required")
    @Pattern(regexp = ValidationPatterns.EXPIRY_MONTH, message = "Expiry month must be between 01 and 12")
    private String expiryMonth;

    @NotBlank(message = "Expiry year is required")
    @Pattern(regexp = ValidationPatterns.EXPIRY_YEAR, message = "Expiry year must be 2 digits (e.g. 28)")
    private String expiryYear;

    @NotBlank(message = "CVC is required")
    @Pattern(regexp = "\\d{3}", message = "CVC must be exactly 3 digits")
    private String cardCvc;

    @AssertTrue(message = "Card expiry date must be in the future")
    public boolean isCardExpiryInFuture() {
        return CardValidationUtils.isExpiryInFutureIfFormatted(expiryMonth, expiryYear);
    }
}
