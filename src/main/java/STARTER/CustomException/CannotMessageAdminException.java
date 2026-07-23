package STARTER.CustomException;

public class CannotMessageAdminException extends RuntimeException {

    public CannotMessageAdminException(String message) {
        super(message);
    }
}
