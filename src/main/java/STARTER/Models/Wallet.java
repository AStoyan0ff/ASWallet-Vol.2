package STARTER.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table( name = "wallets")
public class Wallet extends BaseClass {

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "EUR";

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "receiverWallet")
    @Builder.Default
    private List<Transaction> receivedTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "senderWallet")
    @Builder.Default
    private List<Transaction> sentTransactions = new ArrayList<>();

}