package STARTER.CustomException;

// Advanced — admin cannot change another admin's role
public class CannotChangeAdminRoleException extends RuntimeException {

    public CannotChangeAdminRoleException(String message) {
        super(message);
    }
}
