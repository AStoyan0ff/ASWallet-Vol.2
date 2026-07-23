package STARTER.CustomException;

public class CannotChangeSelfRoleException extends RuntimeException {

    public CannotChangeSelfRoleException(String message) {
        super(message);
    }
}
