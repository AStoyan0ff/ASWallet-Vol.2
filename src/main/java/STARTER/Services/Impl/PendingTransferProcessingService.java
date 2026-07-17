package STARTER.Services.Impl;

import STARTER.CustomException.PendingTransferNotFoundException;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Events.TransactionCompletedEvent;
import STARTER.Models.Transaction;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
// Advanced — transactional pending transfer processing (separate bean for @Transactional proxy)
public class PendingTransferProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PendingTransferProcessingService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationCacheEviction cacheEviction;
    private final TransferRefundSupport transferRefundSupport;
    private final PendingTransferProcessingService self;

    @Value("${app.transfer.processing-delay-seconds:5}")
    private int processingDelaySeconds;

    @Value("${app.transfer.failure-rate:0.1}")
    private double failureRate;

    @Value("${app.transfer.stale-minutes:15}")
    private int staleMinutes;

    public PendingTransferProcessingService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            ApplicationEventPublisher eventPublisher,
            ApplicationCacheEviction cacheEviction,
            TransferRefundSupport transferRefundSupport,
            @Lazy PendingTransferProcessingService self) {

        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.cacheEviction = cacheEviction;
        this.transferRefundSupport = transferRefundSupport;
        this.self = self;
    }

    public void processReadyPendingTransfers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(processingDelaySeconds);

        List<Transaction> pending = transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                TransactionStatus.PENDING,
                TransactionType.TRANSFER,
                threshold
        );

        for (Transaction transaction : pending) {

            try {
                self.finalizePendingTransfer(transaction.getId());

            } catch (RuntimeException ignored) {
                // Transfer may have been canceled or processed concurrently.
            }
        }

        if (!pending.isEmpty()) {
            logger.info("Processed {} ready pending transfer(s)", pending.size());
        }
    }

    public void cancelStalePendingTransfers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleMinutes);

        List<Transaction> stale = transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                TransactionStatus.PENDING,
                TransactionType.TRANSFER,
                threshold
        );

        for (Transaction transaction : stale) {
            try {
                self.cancelPendingTransfer(transaction.getId());

            } catch (RuntimeException ignored) {
                // Transfer may have been processed concurrently.
            }
        }

        if (!stale.isEmpty()) {
            logger.info("Cancelled {} stale pending transfer(s)", stale.size());
        }
    }

    @Transactional
    public void finalizePendingTransfer(UUID transactionId) {
        Transaction transaction = findPendingTransfer(transactionId);

        if (transaction == null) {
            return;
        }

        if (shouldSimulateFailure()) {
            abortPendingTransfer(transaction, TransactionStatus.FAILED);

            logger.info("Pending transfer failed: txId={}, sender={}",
                    transactionId,
                    transaction.getSenderWallet().getUser().getUsername());
            return;
        }

        completeTransfer(transaction);

        logger.info("Pending transfer completed: txId={}, sender={}, receiver={}, amount={}", transactionId,
                transaction.getSenderWallet().getUser().getUsername(),
                transaction.getReceiverWallet().getUser().getUsername(),
                transaction.getAmount()
        );
    }

    @Transactional
    public void cancelPendingTransfer(UUID transactionId) {
        Transaction transaction = findCancellableTransfer(transactionId);

        if (transaction == null) {
            return;
        }

        abortPendingTransfer(transaction, TransactionStatus.CANCELLED);

        logger.info("Pending transfer cancelled: txId={}, sender={}",
                transactionId,
                transaction.getSenderWallet().getUser().getUsername());
    }

    @Transactional
    public boolean approveRiskHeldTransfer(UUID transactionId) {
        Transaction transaction = findRiskHeldTransfer(transactionId);

        if (transaction == null) {
            logger.warn("Risk-held transfer {} not found or already processed", transactionId);
            return false;
        }

        completeTransfer(transaction);

        logger.info("Risk-held transfer approved and completed: txId={}, sender={}, receiver={}, amount={}",
                transactionId,
                transaction.getSenderWallet().getUser().getUsername(),
                transaction.getReceiverWallet().getUser().getUsername(),
                transaction.getAmount());
        return true;
    }

    @Transactional
    public boolean rejectRiskHeldTransfer(UUID transactionId) {
        Transaction transaction = findRiskHeldTransfer(transactionId);

        if (transaction == null) {
            logger.warn("Risk-held transfer {} not found or already processed", transactionId);
            return false;
        }

        abortPendingTransfer(transaction, TransactionStatus.CANCELLED);

        logger.info("Risk-held transfer rejected and refunded: txId={}, sender={}",
                transactionId,
                transaction.getSenderWallet().getUser().getUsername());
        return true;
    }

    private Transaction findPendingTransfer(UUID transactionId) {

        return findTransferByStatus(transactionId, TransactionStatus.PENDING);
    }

    private Transaction findRiskHeldTransfer(UUID transactionId) {

        return transactionRepository.findById(transactionId)
                .filter(transaction -> transaction.getType() == TransactionType.TRANSFER)
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING_RISK_REVIEW)
                .orElse(null);
    }

    private Transaction findCancellableTransfer(UUID transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() ->
                new PendingTransferNotFoundException("Pending transfer not found."));

        if (transaction.getType() != TransactionType.TRANSFER) {
            return null;
        }

        if (transaction.getStatus() != TransactionStatus.PENDING &&
            transaction.getStatus() != TransactionStatus.PENDING_RISK_REVIEW) {

            return null;
        }

        return transaction;
    }

    private Transaction findTransferByStatus(UUID transactionId, TransactionStatus status) {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() ->
                new PendingTransferNotFoundException("Pending transfer not found."));

        if (transaction.getType() != TransactionType.TRANSFER ||
            transaction.getStatus() != status) {
            
                return null;
        }

        return transaction;
    }

    private void completeTransfer(Transaction transaction) {

        Wallet receiverWallet = transaction.getReceiverWallet();
        Wallet senderWallet = transaction.getSenderWallet();

        receiverWallet.setBalance(receiverWallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(receiverWallet);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        cacheEviction.evictTransactionHistoryForWallets(senderWallet, receiverWallet);

        publishTransactionEvent(
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getSenderWallet().getUser(),
                receiverWallet.getUser());
    }

    private void abortPendingTransfer(Transaction transaction, TransactionStatus status) {
        transferRefundSupport.refundSenderAndSetStatus(transaction, status);

        cacheEviction.evictTransactionHistoryForWallets(
            transaction.getSenderWallet(),
            transaction.getReceiverWallet());
    }

    private boolean shouldSimulateFailure() {

        if (failureRate <= 0) {
            return false;
        }

        if (failureRate >= 1) {
            return true;
        }

        return ThreadLocalRandom.current().nextDouble() < failureRate;
    }

    private void publishTransactionEvent(
            BigDecimal amount,
            String description,
            User senderUser,
            User receiverUser) {

        eventPublisher.publishEvent(new TransactionCompletedEvent(
                TransactionType.TRANSFER,
                amount,
                description != null
                        ? description
                        : "-",
                senderUser.getEmail(),
                senderUser.getUsername(),
                receiverUser.getEmail(),
                receiverUser.getUsername()
        ));
    }
}
