package STARTER.CustomException;

public class CannotChangeSelfAccountStatusException extends RuntimeException {

    public CannotChangeSelfAccountStatusException(String message) {
        super(message);
    }
}
