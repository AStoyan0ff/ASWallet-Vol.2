package STARTER.Services.Interface;

import java.math.BigDecimal;

public interface EmailService {

    void sendRegistrationSuccessfulEmail(String to, String username);
    void sendPasswordResetEmail(String to, String username, String resetLink);
    void sendDepositCompletedEmail(String to, String username, BigDecimal amount, String description);
    void sendWithdrawCompletedEmail(String to, String username, BigDecimal amount, String description);
    void sendTransferSentEmail(String to, String username, BigDecimal amount, String receiverUsername, String description);
    void sendTransferReceivedEmail(String to, String username, BigDecimal amount, String senderUsername, String description);
}
