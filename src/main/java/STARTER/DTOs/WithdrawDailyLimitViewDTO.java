package STARTER.DTOs;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawDailyLimitViewDTO {

    private boolean applies;
    private BigDecimal dailyLimit;
    private BigDecimal withdrawnToday;
    private BigDecimal remainingToday;
}
