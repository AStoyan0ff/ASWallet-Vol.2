package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SpendingInsightsViewDTO {

    private String period;
    private String periodLabel;
    private BigDecimal totalSpent;
    private long transactionCount;
    private List<SpendingCategorySliceDTO> categories;
    private boolean empty;
}
