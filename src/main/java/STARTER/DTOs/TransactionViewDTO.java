package STARTER.DTOs;

import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private AccountStatus senderAccountStatus;

    private UUID receiverWalletId;
    private String receiverUsername;
    private AccountStatus receiverAccountStatus;
}
