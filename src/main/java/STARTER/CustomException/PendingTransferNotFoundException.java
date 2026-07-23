package STARTER.CustomException;

public class PendingTransferNotFoundException extends RuntimeException {

    public PendingTransferNotFoundException(String message) {
        super(message);
    }
}
