package STARTER.Enums;

import lombok.Getter;

@Getter
public enum SpendingCategory {

    FOOD("Food"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    ENTERTAINMENT("Entertainment"),
    TRANSPORT("Transport");

    private final String label;

    SpendingCategory(String label) {
        this.label = label;
    }
}
