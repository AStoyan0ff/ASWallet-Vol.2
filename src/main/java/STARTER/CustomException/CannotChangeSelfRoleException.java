package STARTER.CustomException;

// Advanced — admin cannot change own role
public class CannotChangeSelfRoleException extends RuntimeException {

    public CannotChangeSelfRoleException(String message) {
        super(message);
    }
}
