package STARTER.Clients.Dto;

import STARTER.Enums.AssessmentStatus;
import STARTER.Enums.RiskDecision;
import STARTER.Enums.RiskLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentClientResponse {

    private UUID id;
    private UUID transactionRef;
    private String senderUsername;
    private String receiverUsername;
    private BigDecimal amount;
    private int riskScore;
    private RiskLevel riskLevel;
    private RiskDecision decision;
    private AssessmentStatus status;
    private List<String> reasons;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
