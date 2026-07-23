package STARTER.CustomException;

import java.math.BigDecimal;

public class DailyWithdrawLimitExceededException extends RuntimeException {

    private final BigDecimal remainingToday;

    public DailyWithdrawLimitExceededException(BigDecimal remainingToday) {
        super(buildMessage(remainingToday));
        this.remainingToday = remainingToday;
    }

    public BigDecimal getRemainingToday() {
        return remainingToday;
    }

    private static String buildMessage(BigDecimal remainingToday) {

        if (remainingToday == null || remainingToday.signum() <= 0) {
            return "Daily withdraw limit reached. Try again tomorrow.";
        }

        return "Daily withdraw limit exceeded. Remaining today: €" + remainingToday.stripTrailingZeros().toPlainString();
    }
}
