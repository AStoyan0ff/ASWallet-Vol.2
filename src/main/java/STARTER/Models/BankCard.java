package STARTER.Models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "bank_cards")
public class BankCard extends BaseClass {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "last_four_digits", nullable = false)
    private String lastFourDigits;

    @Column(name = "cardholder_name", nullable = false)
    private String cardholderName;

    @Column(name = "expiry_month", nullable = false)
    private String expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private String expiryYear;

    // length = 22
    @Column(name = "iban", unique = true)
    private String iban;
}
