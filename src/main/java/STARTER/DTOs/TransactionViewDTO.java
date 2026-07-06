package STARTER.DTOs;

import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor

public class TransactionViewDTO {

    private UUID id;
    private BigDecimal amount;
    private String description;
    private String createdAt;

    private TransactionStatus status;
    private TransactionType type;

    private UUID senderWalletId;
    private String senderUsername;
    // Advanced: sender account status in transaction history
    private AccountStatus senderAccountStatus;

    private UUID receiverWalletId;
    private String receiverUsername;
    // Advanced: receiver account status in transaction history
    private AccountStatus receiverAccountStatus;
}
