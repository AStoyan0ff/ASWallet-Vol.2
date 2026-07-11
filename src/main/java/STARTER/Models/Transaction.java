package STARTER.Models;

import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor

@Table( name = "transactions")
public class Transaction extends BaseClass implements Persistable<UUID> {

    @Transient
    private boolean newEntity = true;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
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

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        this.newEntity = false;
    }

    @Override
    protected void onPrePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = TransactionStatus.COMPLETED;
        }
    }
}