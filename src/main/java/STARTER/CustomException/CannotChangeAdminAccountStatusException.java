package STARTER.CustomException;

public class CannotChangeAdminAccountStatusException extends RuntimeException {

    public CannotChangeAdminAccountStatusException(String message) {
        super(message);
    }
}
