package STARTER.CustomException;

public class TransferBlockedByRiskException extends RuntimeException {

    public TransferBlockedByRiskException(String message) {
        super(message);
    }
}
