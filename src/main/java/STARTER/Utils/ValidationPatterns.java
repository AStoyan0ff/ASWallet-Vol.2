package STARTER.Utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationPatterns {

    public static final String USERNAME =
            "^[a-zA-Z][a-zA-Z0-9_]{2,29}$";

    public static final String EMAIL =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String PASSWORD =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$";

    public static final String CARD_NUMBER = "\\d{16}";

    public static final String CARDHOLDER_NAME =
            "^[A-Za-z][A-Za-z '\\- ]{1,79}$";

    public static final String EXPIRY_MONTH = "^(0[1-9]|1[0-2])$";

    public static final String EXPIRY_YEAR = "^\\d{2}$";

//    public static final String OPTIONAL_DESCRIPTION =
//            "^$|^[\\p{L}\\p{N}\\s',.\\-!?()]{1,200}$";

    // Advanced: profile validation patterns (Latin + Cyrillic and other Unicode letters)
    public static final String OPTIONAL_PERSON_NAME =
            "^$|^\\p{L}[\\p{L} '\\- ]{0,49}$";

    public static final String OPTIONAL_PHONE =
            "^$|\\+?[0-9]{7,15}$";

//    public static final String OPTIONAL_AVATAR_URL =
//            "^$|(https?://.+|/images/.+|images/.+)$";

    public static final String RESET_TOKEN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public static final String MAX_TRANSACTION_AMOUNT = "999999.99";
}
