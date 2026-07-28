package STARTER.Models;

import STARTER.Enums.MoneyRequestStatus;
import STARTER.Enums.SpendingCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "money_requests")
public class MoneyRequest extends BaseClass {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "spending_category", nullable = false)
    private SpendingCategory spendingCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MoneyRequestStatus status = MoneyRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Override
    protected void onPrePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = MoneyRequestStatus.PENDING;
        }
    }
}
