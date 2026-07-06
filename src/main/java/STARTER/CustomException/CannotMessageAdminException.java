package STARTER.CustomException;

// Advanced — mailbox messages can only be sent to regular users
public class CannotMessageAdminException extends RuntimeException {

    public CannotMessageAdminException(String message) {
        super(message);
    }
}
