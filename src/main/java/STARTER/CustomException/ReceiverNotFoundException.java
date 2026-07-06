package STARTER.CustomException;

public class ReceiverNotFoundException extends RuntimeException {

    public ReceiverNotFoundException(String message) {
        super(message);
    }
}
