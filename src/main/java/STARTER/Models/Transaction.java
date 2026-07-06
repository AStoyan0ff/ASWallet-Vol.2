package STARTER.Models;

import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor

@Table( name = "transactions")
public class Transaction extends BaseClass {

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;

    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;

    @PrePersist
    public void prePersist() {
         this.createdAt = LocalDateTime.now();

         if (this.status == null) {
             this.status = TransactionStatus.COMPLETED;
         }
    }
}