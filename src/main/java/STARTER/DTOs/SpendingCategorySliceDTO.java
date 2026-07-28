package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SpendingCategorySliceDTO {

    private String category;
    private BigDecimal amount;
    private double percent;
    private String color;
}
