package STARTER.Utils;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Predicate;

@UtilityClass
public class IbanGenerator {

    public static final String COUNTRY_CODE = "BG";
    public static final String BANK_CODE = "ASWL";
    public static final int IBAN_LENGTH = 22;


    public String generate(UUID userId, Predicate<String> uniquenessCheck) {

        for (int attempt = 0; attempt < 20; attempt++) {

            String accountDigits = deriveAccountDigits(userId, attempt);
            String iban = buildIban(accountDigits);

            if (uniquenessCheck.test(iban)) {
                return iban;
            }
        }

        throw new IllegalStateException("Unable to generate unique IBAN");
    }

    public String formatForDisplay(String iban) {

        if (iban == null || iban.length() != IBAN_LENGTH) {
            return iban;
        }

        return String.format(
                "%s %s %s %s %s %s",
                iban.substring(0, 4),
                iban.substring(4, 8),
                iban.substring(8, 12),
                iban.substring(12, 16),
                iban.substring(16, 20),
                iban.substring(20, 22)
        );
    }

    private String buildIban(String accountDigits) {

        String bban = BANK_CODE + accountDigits;
        String checkDigits = computeCheckDigits(bban);

        return COUNTRY_CODE + checkDigits + bban;
    }

    private String deriveAccountDigits(UUID userId, int attempt) {

        long seed = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits() ^ attempt;
        long value = Math.floorMod(seed, 10_000_000_000_000L);

        return String.format("%014d", value);
    }

    private String computeCheckDigits(String bban) {

        String rearranged = bban + COUNTRY_CODE + "00";
        String numeric = toNumericString(rearranged);
        int remainder = mod97(numeric);
        int checkDigits = 98 - remainder;
        return String.format("%02d", checkDigits);
    }

    private String toNumericString(String value) {

        StringBuilder builder = new StringBuilder(value.length() * 2);

        for (char character : value.toCharArray()) {

            if (Character.isDigit(character)) {
                builder.append(character);

            } else if (Character.isLetter(character)) {
                builder.append(Character.toUpperCase(character) - 'A' + 10);

            } else {
                throw new IllegalArgumentException("Invalid IBAN character: " + character);
            }
        }

        return builder.toString();
    }

    private int mod97(String numericValue) {

        BigInteger remainder = BigInteger.ZERO;

        for (int index = 0; index < numericValue.length(); index += 7) {

            String chunk = remainder + numericValue.substring(index, Math.min(index + 7, numericValue.length()));
            remainder = new BigInteger(chunk).remainder(BigInteger.valueOf(97));
        }

        return remainder.intValue();
    }
}
