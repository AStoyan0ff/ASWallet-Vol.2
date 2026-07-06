package STARTER.Services.Impl;

import STARTER.Services.Interface.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendRegistrationSuccessfulEmail(String to, String username) {

        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("ASWallet - Successful registration");
        msg.setText(
        """
        Hello %s,

        Your ASWallet account has been successfully created.

        We're happy to have you with us!

        Regards,
        ASWallet Team
        """
            .formatted(username));

        sendSafely(msg);
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetLink) {

        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("ASWallet - Reset your password");
        msg.setText(
        """
        Hello %s,

        We received a request to reset your ASWallet password.

        Click the link below to choose a new password:
        %s

        This link expires in 1 hour and can only be used once.

        If you did not request a password reset, you can safely ignore this email.

        Regards,
        ASWallet Team
        """
            .formatted(username, resetLink));

        sendSafely(msg);
    }

    @Override
    public void sendDepositCompletedEmail(String to, String username, BigDecimal amount, String description) {
        sendTransactionEmail(
                to,
                "ASWallet - Deposit successful",
                """
                Hello %s,

                Your deposit of %s EUR was completed successfully.

                Details: %s

                Regards,
                ASWallet Team
                """.formatted(username, amount, description)
        );
    }

    @Override
    public void sendWithdrawCompletedEmail(String to, String username, BigDecimal amount, String description) {
        sendTransactionEmail(
                to,
                "ASWallet - Withdrawal successful",
                """
                Hello %s,

                Your withdrawal of %s EUR was completed successfully.

                Details: %s

                Regards,
                ASWallet Team
                """.formatted(username, amount, description)
        );
    }

    @Override
    public void sendTransferSentEmail(
            String to,
            String username,
            BigDecimal amount,
            String receiverUsername,
            String description
    ) {
        sendTransactionEmail(
                to,
                "ASWallet - Transfer sent",
                """
                Hello %s,

                You sent %s EUR to %s.

                Details: %s

                Regards,
                ASWallet Team
                """.formatted(username, amount, receiverUsername, description)
        );
    }

    @Override
    public void sendTransferReceivedEmail(
            String to,
            String username,
            BigDecimal amount,
            String senderUsername,
            String description
    ) {
        sendTransactionEmail(
                to,
                "ASWallet - Transfer received",
                """
                Hello %s,

                You received %s EUR from %s.

                Details: %s

                Regards,
                ASWallet Team
                """.formatted(username, amount, senderUsername, description)
        );
    }

    private void sendTransactionEmail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        sendSafely(msg);
    }

    private void sendSafely(SimpleMailMessage msg) {

        try {
            mailSender.send(msg);

        } catch (RuntimeException ex) {
            logger.warn("Failed to send email to {} with subject '{}': {}",
                    msg.getTo(), msg.getSubject(), ex.getMessage());
        }
    }
}
