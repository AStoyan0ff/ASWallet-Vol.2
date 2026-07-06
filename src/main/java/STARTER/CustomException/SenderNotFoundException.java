package STARTER.CustomException;

public class SenderNotFoundException extends RuntimeException {

    public SenderNotFoundException(String message) {
        super(message);
    }
}
