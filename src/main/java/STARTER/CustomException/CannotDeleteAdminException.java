package STARTER.CustomException;

public class CannotDeleteAdminException extends RuntimeException {

    public CannotDeleteAdminException(String message) {
        super(message);
    }
}
