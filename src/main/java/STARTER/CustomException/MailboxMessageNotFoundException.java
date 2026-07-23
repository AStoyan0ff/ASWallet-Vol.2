package STARTER.CustomException;

public class MailboxMessageNotFoundException extends RuntimeException {

    public MailboxMessageNotFoundException(String message) {
        super(message);
    }
}
