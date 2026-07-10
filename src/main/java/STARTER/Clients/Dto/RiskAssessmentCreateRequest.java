package STARTER.Clients.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentCreateRequest {

    private UUID transactionRef;
    private String senderUsername;
    private String receiverUsername;
    private BigDecimal amount;
    private BigDecimal senderBalance;
    private BigDecimal withdrawnToday;
    private BigDecimal dailyLimit;
    private int transfersTodayCount;
    private boolean receiverHasBankCard;
    private boolean newReceiver;
    private String accountStatus;
    private int hourOfDay;
}
