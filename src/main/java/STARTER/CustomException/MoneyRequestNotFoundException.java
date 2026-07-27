package STARTER.CustomException;

public class MoneyRequestNotFoundException extends RuntimeException {

    public MoneyRequestNotFoundException(String message) {
        super(message);
    }
}
