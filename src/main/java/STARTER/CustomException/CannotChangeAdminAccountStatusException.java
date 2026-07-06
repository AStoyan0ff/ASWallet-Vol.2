package STARTER.CustomException;

// Advanced - admin cannot change admin account status
public class CannotChangeAdminAccountStatusException extends RuntimeException {

    public CannotChangeAdminAccountStatusException(String message) {
        super(message);
    }
}
