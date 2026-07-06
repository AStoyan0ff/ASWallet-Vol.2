package STARTER.CustomException;

public class ReceiverBankCardNotFoundException extends RuntimeException {

    public ReceiverBankCardNotFoundException(String message) {
        super(message);
    }
}
