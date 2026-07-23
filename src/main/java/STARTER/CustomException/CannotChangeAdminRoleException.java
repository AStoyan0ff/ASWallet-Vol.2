package STARTER.CustomException;

public class CannotChangeAdminRoleException extends RuntimeException {

    public CannotChangeAdminRoleException(String message) {
        super(message);
    }
}
