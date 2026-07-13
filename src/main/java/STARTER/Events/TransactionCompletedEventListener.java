package STARTER.Events;

import STARTER.Enums.TransactionType;
import STARTER.Services.Interface.EmailService;
import STARTER.Services.Interface.UserProfileDetailsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionCompletedEventListener {

    private final EmailService emailService;
    private final UserProfileDetailsService userProfileDetailsService;

    public TransactionCompletedEventListener(EmailService emailService, UserProfileDetailsService userProfileDetailsService) {

        this.emailService = emailService;
        this.userProfileDetailsService = userProfileDetailsService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(TransactionCompletedEvent event) {

        switch (event.type()) {

            case DEPOSIT -> {
                if (isEmailEnabled(event.primaryUsername(), TransactionType.DEPOSIT)) {

                    emailService.sendDepositCompletedEmail(
                            event.primaryEmail(),
                            event.primaryUsername(),
                            event.amount(),
                            event.description()
                    );
                }
            }
            case WITHDRAW -> {
                if (isEmailEnabled(event.primaryUsername(), TransactionType.WITHDRAW)) {

                    emailService.sendWithdrawCompletedEmail(
                            event.primaryEmail(),
                            event.primaryUsername(),
                            event.amount(),
                            event.description()
                    );
                }
            }
            case TRANSFER -> {
                if (isEmailEnabled(event.primaryUsername(), TransactionType.TRANSFER)) {

                    emailService.sendTransferSentEmail(
                            event.primaryEmail(),
                            event.primaryUsername(),
                            event.amount(),
                            event.secondaryUsername(),
                            event.description()
                    );
                }
                if (event.secondaryUsername() != null &&
                         isEmailEnabled(event.secondaryUsername(), TransactionType.TRANSFER)) {

                    emailService.sendTransferReceivedEmail(
                            event.secondaryEmail(),
                            event.secondaryUsername(),
                            event.amount(),
                            event.primaryUsername(),
                            event.description()
                    );
                }
            }
        }
    }

    private boolean isEmailEnabled(String username, TransactionType type) {

        if (username == null || username.isBlank()) {
            return false;
        }

        return userProfileDetailsService.isTransactionEmailEnabled(username, type);
    }
}
