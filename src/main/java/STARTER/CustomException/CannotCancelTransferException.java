package STARTER.CustomException;

public class CannotCancelTransferException extends RuntimeException {

    public CannotCancelTransferException(String message) {
        super(message);
    }
}
