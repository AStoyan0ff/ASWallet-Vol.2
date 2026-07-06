package STARTER.CustomException;

public class CannotDeleteSelfException extends RuntimeException {

    public CannotDeleteSelfException(String message) {
        super(message);
    }
}
