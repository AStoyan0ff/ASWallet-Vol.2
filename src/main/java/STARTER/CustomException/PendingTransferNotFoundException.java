package STARTER.CustomException;

// Advanced - pending transfer not found
public class PendingTransferNotFoundException extends RuntimeException {

    public PendingTransferNotFoundException(String message) {
        super(message);
    }
}
