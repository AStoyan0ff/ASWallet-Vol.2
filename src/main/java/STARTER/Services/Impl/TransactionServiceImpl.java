package STARTER.Services.Impl;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Configuration.CacheConfig;
import STARTER.CustomException.*;
import STARTER.DTOs.*;
import STARTER.Enums.*;
import STARTER.Events.TransactionCompletedEvent;
import STARTER.Models.*;
import STARTER.Repositories.*;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.TransferRiskAssessmentService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import STARTER.Specifications.TransactionSpecifications;
import STARTER.Utils.DateTimeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private static final BigDecimal WELCOME_BONUS_EUR = new BigDecimal("50.00");
    private static final String WELCOME_BONUS_DESCRIPTION = "Welcome Bonus";

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final BankCardRepository bankCardRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PendingTransferProcessingService pendingTransferProcessingService;
    private final ApplicationCacheEviction cacheEviction;
    private final TransferRefundSupport transferRefundSupport;
    private final WithdrawDailyLimitService withdrawDailyLimitService;
    private final TransferRiskAssessmentService transferRiskAssessmentService;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository,
            BankCardRepository bankCardRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            ApplicationEventPublisher eventPublisher,
            PendingTransferProcessingService pendingTransferProcessingService,
            ApplicationCacheEviction cacheEviction,
            TransferRefundSupport transferRefundSupport,
            WithdrawDailyLimitService withdrawDailyLimitService,
            TransferRiskAssessmentService transferRiskAssessmentService
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.bankCardRepository = bankCardRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.eventPublisher = eventPublisher;
        this.pendingTransferProcessingService = pendingTransferProcessingService;
        this.cacheEviction = cacheEviction;
        this.transferRefundSupport = transferRefundSupport;
        this.withdrawDailyLimitService = withdrawDailyLimitService;
        this.transferRiskAssessmentService = transferRiskAssessmentService;
    }

    @Override
    @Transactional
    public void transfer(UUID senderUserId, TransferMoneyDTO transferMoneyDTO) {

        Wallet senderWallet = walletRepository.findByUser_Id(senderUserId).orElseThrow(() ->
            new SenderNotFoundException("Sender wallet not found"));

        User receiverUser = userRepository.findByUsername(transferMoneyDTO.getReceiverUsername()).orElseThrow(() ->
            new UserNotFoundException("Receiver user not found"));

        Wallet receiverWallet = walletRepository.findByUser_Id(receiverUser.getId()).orElseThrow(() ->
            new ReceiverNotFoundException("Receiver wallet not found"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new NotTransferMoneyYourselfException("You cannot transfer money to yourself");
        }

        BankCard receiverCard = bankCardRepository.findByUser_Id(receiverUser.getId()) .orElseThrow(() ->
            new ReceiverBankCardNotFoundException("Receiver has no registered bank card"));

        String receiverCardMask = "****" + receiverCard.getLastFourDigits();

        if (senderWallet.getBalance().compareTo(transferMoneyDTO.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        UUID transactionId = UUID.randomUUID();

        RiskAssessmentClientResponse riskResponse = transferRiskAssessmentService.assessTransfer(
                transactionId,
                senderWallet.getUser(),
                senderWallet,
                receiverUser,
                receiverWallet,
                transferMoneyDTO,
                true
        );

        senderWallet.setBalance(senderWallet.getBalance().subtract(transferMoneyDTO.getAmount()));

        String description = formatSpendingDescription(transferMoneyDTO.getSpendingCategory(),
            " (to card " + receiverCardMask + ")");

        Transaction transaction = new Transaction();

        transaction.setId(transactionId);
        transaction.setAmount(transferMoneyDTO.getAmount());
        transaction.setDescription(description);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(riskResponse.getDecision() == RiskDecision.REVIEW
                ? TransactionStatus.PENDING_RISK_REVIEW
                : TransactionStatus.PENDING);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);

        walletRepository.save(senderWallet);
        transactionRepository.save(transaction);
        cacheEviction.evictTransactionHistoryForWallets(senderWallet, receiverWallet);

        logger.info(
                "Transfer submitted as pending: sender={}, receiver={}, amount={}, txId={}",
                senderWallet.getUser().getUsername(),
                receiverUser.getUsername(),
                transferMoneyDTO.getAmount(),
                transaction.getId()
        );
    }

    @Override
    public void processPendingTransfers() {
        pendingTransferProcessingService.processReadyPendingTransfers();
    }

    @Override
    public void cancelStalePendingTransfers() {
        pendingTransferProcessingService.cancelStalePendingTransfers();
    }

    @Override
    @Transactional
    public void cancelPendingTransfer(UUID transactionId, UUID userId) {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() ->
                new PendingTransferNotFoundException("Pending transfer not found."));

        if (transaction.getType() != TransactionType.TRANSFER ||
           (transaction.getStatus() != TransactionStatus.PENDING &&
            transaction.getStatus() != TransactionStatus.PENDING_RISK_REVIEW)) {
                throw new CannotCancelTransferException("Only pending transfers can be cancelled.");
        }

        Wallet senderWallet = transaction.getSenderWallet();

        if (senderWallet == null || !senderWallet.getUser().getId().equals(userId)) {
            throw new CannotCancelTransferException("You can only cancel transfers that you initiated.");
        }

        pendingTransferProcessingService.cancelPendingTransfer(transactionId);

        logger.info(
                "Pending transfer cancelled by user: userId={}, txId={}",
                userId,
                transactionId
        );
    }

    @Override
    @Transactional
    public void deposit(UUID userID, DepositMoneyDTO depositMoneyDTO) {

        Wallet wallet = walletRepository.findByUser_Id(userID).orElseThrow(() ->
            new WalletNotFoundException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(depositMoneyDTO.getAmount()));

        String maskedCard = bankCardRepository.findByUser_Id(userID)
                .map(card -> "****" + card.getLastFourDigits())
                .orElse("****");

        Transaction transaction = new Transaction();

        transaction.setAmount(depositMoneyDTO.getAmount());
        transaction.setDescription(formatSpendingDescription(
                depositMoneyDTO.getSpendingCategory(),
                " — top-up from card " + maskedCard
        ));

        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setSenderWallet(null);
        transaction.setReceiverWallet(wallet);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        cacheEviction.evictTransactionHistory(userID);

        publishTransactionEvent(
                TransactionType.DEPOSIT,
                depositMoneyDTO.getAmount(),
                transaction.getDescription(),
                wallet.getUser(),
                null
        );

        logger.info(
                "Deposit completed: user={}, amount={}, txId={}",
                wallet.getUser().getUsername(),
                depositMoneyDTO.getAmount(),
                transaction.getId()
        );
    }

    @Override
    @Transactional
    public void withdraw(UUID userID, WithdrawMoneyDTO withdrawMoneyDTO) {

        Wallet wallet = walletRepository.findByUser_Id(userID).orElseThrow(() ->
            new WalletNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(withdrawMoneyDTO.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        withdrawDailyLimitService.assertWithinDailyLimit(userID, withdrawMoneyDTO.getAmount());

        wallet.setBalance(wallet.getBalance().subtract(withdrawMoneyDTO.getAmount()));

        String cardMask = bankCardRepository.findByUser_Id(userID)
                .map(card -> "****" + card.getLastFourDigits())
                .orElse("****");

        String description = formatSpendingDescription(
                withdrawMoneyDTO.getSpendingCategory(),
                " (to card " + cardMask + ")"
        );

        Transaction transaction = new Transaction();

        transaction.setAmount(withdrawMoneyDTO.getAmount());
        transaction.setDescription(description);
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setSenderWallet(wallet);
        transaction.setReceiverWallet(null);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        cacheEviction.evictTransactionHistory(userID);

        publishTransactionEvent(
                TransactionType.WITHDRAW,
                withdrawMoneyDTO.getAmount(),
                description,
                wallet.getUser(),
                null
        );

        logger.info(
                "Withdraw completed: user={}, amount={}, txId={}",
                wallet.getUser().getUsername(),
                withdrawMoneyDTO.getAmount(),
                transaction.getId()
        );
    }

    @Override
    @Transactional
    public void grantWelcomeBonus(UUID userId) {

        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(WELCOME_BONUS_EUR));

        Transaction transaction = new Transaction();

        transaction.setAmount(WELCOME_BONUS_EUR);
        transaction.setDescription(WELCOME_BONUS_DESCRIPTION);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setSenderWallet(null);
        transaction.setReceiverWallet(wallet);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        cacheEviction.evictTransactionHistory(userId);

        publishTransactionEvent(
                TransactionType.DEPOSIT,
                WELCOME_BONUS_EUR,
                WELCOME_BONUS_DESCRIPTION,
                wallet.getUser(),
                null
        );

        logger.info("Welcome bonus granted: user={}, amount={}, txId={}",
                wallet.getUser().getUsername(),
                WELCOME_BONUS_EUR,
                transaction.getId());
    }

    @Override
    @Cacheable(value = CacheConfig.TRANSACTION_HISTORY, key = "#userID")
    public List<TransactionViewDTO> getUserTransactions(UUID userID) {
        return getFilteredUserTransactions(userID, new TransactionHistoryFilter());
    }

    @Override
    public Page<TransactionViewDTO> getUserTransactionsPage(UUID userId, int page, int size) {
        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 5;

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Transaction> specification = TransactionSpecifications.forUserWallet(wallet, new TransactionHistoryFilter());
        Map<String, AccountStatus> accountStatusCache = new HashMap<>();
        Page<Transaction> transactionPage = transactionRepository.findAll(specification, pageable);

        if (transactionPage.isEmpty() && safePage > 0 && transactionPage.getTotalPages() > 0) {
            int lastPage = transactionPage.getTotalPages() - 1;

            pageable = PageRequest.of(lastPage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            transactionPage = transactionRepository.findAll(specification, pageable);
        }

        return transactionPage.map(transaction -> mapToEntity(transaction, accountStatusCache));
    }

    @Override
    public boolean hasPendingTransfers(UUID userId) {

        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        return transactionRepository.existsByWalletInvolvedAndStatusIn(wallet.getId(),
                List.of(TransactionStatus.PENDING, TransactionStatus.PENDING_RISK_REVIEW));
    }

    @Override
    @Transactional
    public void clearUserTransactionHistory(UUID userId) {

        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        List<Transaction> transactions = transactionRepository.findAllBySenderWalletOrReceiverWallet(wallet, wallet);

        for (Transaction transaction : transactions) {

            if ((transaction.getStatus() == TransactionStatus.PENDING ||
                 transaction.getStatus() == TransactionStatus.PENDING_RISK_REVIEW) &&
                 transaction.getType() == TransactionType.TRANSFER &&
                 transaction.getSenderWallet() != null &&
                 wallet.getId().equals(transaction.getSenderWallet().getId())) {
                     transferRefundSupport.refundSenderAndSetStatus(transaction, TransactionStatus.CANCELLED);
            }
        }

        if (!transactions.isEmpty()) {
            transactionRepository.deleteAll(transactions);
        }

        cacheEviction.evictTransactionHistory(userId);
    }

    @Override
    public List<TransactionViewDTO> getFilteredUserTransactions(UUID userId, TransactionHistoryFilter filter) {

        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        TransactionHistoryFilter safeFilter = filter != null ? filter : new TransactionHistoryFilter();
        Specification<Transaction> specification = TransactionSpecifications.forUserWallet(wallet, safeFilter);
        Map<String, AccountStatus> accountStatusCache = new HashMap<>();

        return transactionRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(transaction -> mapToEntity(transaction, accountStatusCache))
                .toList();
    }

    private TransactionViewDTO mapToEntity(Transaction transaction, Map<String, AccountStatus> accountStatusCache) {
        TransactionViewDTO dto = new TransactionViewDTO();

        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(toCategoryLabel(transaction.getDescription()));
        dto.setCreatedAt(DateTimeDisplay.format(transaction.getCreatedAt()));
        dto.setStatus(transaction.getStatus());
        dto.setType(transaction.getType());

        if (transaction.getSenderWallet() != null) {
            String senderUsername = transaction.getSenderWallet().getUser().getUsername();

            dto.setSenderWalletId(transaction.getSenderWallet().getId());
            dto.setSenderUsername(senderUsername);
            dto.setSenderAccountStatus(resolveAccountStatus(senderUsername, accountStatusCache));
        }

        if (transaction.getReceiverWallet() != null) {
            String receiverUsername = transaction.getReceiverWallet().getUser().getUsername();

            dto.setReceiverWalletId(transaction.getReceiverWallet().getId());
            dto.setReceiverUsername(receiverUsername);
            dto.setReceiverAccountStatus(resolveAccountStatus(receiverUsername, accountStatusCache));
        }

        return dto;
    }

    private AccountStatus resolveAccountStatus(String username, Map<String, AccountStatus> accountStatusCache) {

        return accountStatusCache.computeIfAbsent(username, key ->
                profileDetailsRepository.findByUser_Username(key)
                        .map(UserProfileDetails::getAccountStatus)
                        .orElse(AccountStatus.ACTIVE)
        );
    }

    private String formatSpendingDescription(SpendingCategory category, String suffix) {
        return category.getLabel() + suffix;
    }

    private String toCategoryLabel(String description) {

        if (description == null || description.isBlank()) {
            return description;
        }

        if (description.regionMatches(true, 0, "Welcome bonus", 0, "Welcome bonus".length())) {
            return WELCOME_BONUS_DESCRIPTION;
        }

        for (SpendingCategory category : SpendingCategory.values()) {
            String label = category.getLabel();

            if (description.equals(label)) {
                return label;
            }
            if (description.startsWith(label) && description.length() > label.length()) {
                char next = description.charAt(label.length());

                if (next == ' ' || next == '(' || next == '—' || next == '–' || next == '-') {
                    return label;
                }
            }
        }
        return description;
    }

    private void publishTransactionEvent(
            TransactionType type,
            BigDecimal amount,
            String description,
            User primaryUser,
            User secondaryUser) {

        eventPublisher.publishEvent(new TransactionCompletedEvent(
                type,
                amount,
                description != null ? description : "-",
                primaryUser.getEmail(),
                primaryUser.getUsername(),
                secondaryUser != null
                    ? secondaryUser.getEmail()
                    : null,
                secondaryUser != null
                    ? secondaryUser.getUsername()
                    : null
        ));
    }
}
