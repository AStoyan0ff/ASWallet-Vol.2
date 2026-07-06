package STARTER.CustomException;

public class InvalidCardDetailsException extends RuntimeException {

    public InvalidCardDetailsException(String message) {
        super(message);
    }
}
