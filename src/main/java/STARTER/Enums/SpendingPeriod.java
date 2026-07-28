package STARTER.Enums;

import lombok.Getter;

@Getter
public enum SpendingPeriod {

    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    ALL_TIME("All time");

    private final String label;

    SpendingPeriod(String label) {
        this.label = label;
    }

    public static SpendingPeriod fromParam(String value) {
        if (value == null || value.isBlank()) {
            return THIS_WEEK;
        }

        try {
            return SpendingPeriod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return THIS_WEEK;
        }
    }
}
