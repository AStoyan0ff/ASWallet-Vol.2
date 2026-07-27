package STARTER.CustomException;

public class MoneyRequestActionNotAllowedException extends RuntimeException {

    public MoneyRequestActionNotAllowedException(String message) {
        super(message);
    }
}
