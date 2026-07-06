package STARTER.DTOs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankCardViewDTO {

    private String maskedCardNumber;
    private String lastFourDigits;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private String iban;
    private String formattedIban;
}
