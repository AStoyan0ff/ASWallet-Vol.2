package STARTER.CustomException;

// Advanced — pending transfer cancel restrictions
public class CannotCancelTransferException extends RuntimeException {

    public CannotCancelTransferException(String message) {
        super(message);
    }
}
