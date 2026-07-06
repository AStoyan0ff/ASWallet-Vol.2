package STARTER.DTOs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class DailyLimitEditRequest {

    private BigDecimal dailyWithdrawLimit;
}
