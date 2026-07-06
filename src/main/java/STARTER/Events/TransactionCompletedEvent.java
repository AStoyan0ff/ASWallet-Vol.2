package STARTER.Events;

import STARTER.Enums.TransactionType;

import java.math.BigDecimal;

public record TransactionCompletedEvent(
        TransactionType type,
        BigDecimal amount,
        String description,
        String primaryEmail,
        String primaryUsername,
        String secondaryEmail,
        String secondaryUsername
) {}
