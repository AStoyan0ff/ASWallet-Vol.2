package STARTER.CustomException;

// Advanced - admin cannot change own account status
public class CannotChangeSelfAccountStatusException extends RuntimeException {

    public CannotChangeSelfAccountStatusException(String message) {
        super(message);
    }
}
