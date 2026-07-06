package STARTER.CustomException;

// Advanced — mailbox message not found for recipient
public class MailboxMessageNotFoundException extends RuntimeException {

    public MailboxMessageNotFoundException(String message) {
        super(message);
    }
}
