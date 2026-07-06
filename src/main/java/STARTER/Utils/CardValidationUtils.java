package STARTER.Utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CardValidationUtils {

    public static boolean isExpiryInFuture(String expiryMonth, String expiryYear) {

        int month = Integer.parseInt(expiryMonth);
        int year = 2000 + Integer.parseInt(expiryYear);
        int currentYear = java.time.YearMonth.now().getYear();
        int currentMonth = java.time.YearMonth.now().getMonthValue();

        if (year > currentYear) {
            return true;
        }

        return year == currentYear && month >= currentMonth;
    }

    public static boolean isExpiryInFutureIfFormatted(String expiryMonth, String expiryYear) {

        if (expiryMonth == null || expiryYear == null
                || expiryMonth.isBlank() || expiryYear.isBlank()) {
            return true;
        }
        if (!expiryMonth.matches(ValidationPatterns.EXPIRY_MONTH)
                || !expiryYear.matches(ValidationPatterns.EXPIRY_YEAR)) {
            return true;
        }
        return isExpiryInFuture(expiryMonth, expiryYear);
    }
}
