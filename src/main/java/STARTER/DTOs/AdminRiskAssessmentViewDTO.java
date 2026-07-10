package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AdminRiskAssessmentViewDTO {

    private UUID id;
    private String senderUsername;
    private String receiverUsername;
    private BigDecimal amount;
    private int riskScore;
    private String riskLevel;
    private String decision;
    private String createdAt;
    private List<String> reasons;
}
