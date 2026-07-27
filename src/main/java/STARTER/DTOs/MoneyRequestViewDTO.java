package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class MoneyRequestViewDTO {

    private UUID id;
    private String requesterUsername;
    private String payerUsername;
    private BigDecimal amount;
    private String spendingCategory;
    private String note;
    private String status;
    private String createdAt;
    private String resolvedAt;
    private boolean incoming;
    private boolean canRespond;
    private boolean canCancel;
}
