package STARTER.Scheduling;

import STARTER.Services.Interface.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingTransferScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PendingTransferScheduler.class);
    private final TransactionService transactionService;

    public PendingTransferScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(cron = "${app.transfer.process.cron:0 */1 * * * *}")
    public void processPendingTransfers() {

        logger.debug("Running pending transfer processing job");
        transactionService.processPendingTransfers();
    }

    @Scheduled(fixedDelayString = "${app.transfer.stale-check.fixed-delay-ms:300000}")
    public void cancelStalePendingTransfers() {

        logger.debug("Running stale pending transfer cleanup job");
        transactionService.cancelStalePendingTransfers();
    }
}
