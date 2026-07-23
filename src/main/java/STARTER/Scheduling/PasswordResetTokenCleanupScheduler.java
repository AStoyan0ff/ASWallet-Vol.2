package STARTER.Scheduling;

import STARTER.Services.Interface.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenCleanupScheduler.class);
    private final PasswordResetService passwordResetService;

    public PasswordResetTokenCleanupScheduler(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Scheduled(cron = "${app.password-reset.cleanup.cron:0 0 * * * *}")
    public void cleanupExpiredTokens() {

        logger.debug("Running password reset token cleanup job");
        passwordResetService.cleanupExpiredAndUsedTokens();
    }
}
