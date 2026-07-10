package STARTER.CustomException;

public class RiskReviewServiceException extends RuntimeException {

    public RiskReviewServiceException(String message) {
        super(message);
    }
}
