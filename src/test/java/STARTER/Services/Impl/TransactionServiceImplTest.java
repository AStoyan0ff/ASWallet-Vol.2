package STARTER.Services.Impl;

import STARTER.CustomException.*;
import STARTER.DTOs.*;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.SpendingCategory;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Events.TransactionCompletedEvent;
import STARTER.Models.*;
import STARTER.Repositories.*;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private BankCardRepository bankCardRepository;
    @Mock private UserProfileDetailsRepository profileDetailsRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PendingTransferProcessingService pendingTransferProcessingService;
    @Mock private ApplicationCacheEviction cacheEviction;
    @Mock private TransferRefundSupport transferRefundSupport;
    @Mock private WithdrawDailyLimitService withdrawDailyLimitService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID userId;
    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("Plamen");
        user.setEmail("plamen@example.com");

        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setCurrency("EUR");
    }

    // --- DEPOSIT ---

    @Test
    void deposit_increasesBalance_andSavesCompletedTransaction() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(bankCardRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        DepositMoneyDTO dto = new DepositMoneyDTO();
        dto.setAmount(new BigDecimal("25.00"));
        dto.setCardCvc("123");
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        transactionService.deposit(userId, dto);

        assertThat(wallet.getBalance()).isEqualByComparingTo("125.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getAmount()).isEqualByComparingTo("25.00");
        assertThat(saved.getReceiverWallet()).isEqualTo(wallet);
        assertThat(saved.getSenderWallet()).isNull();

        verify(walletRepository).save(wallet);
        verify(cacheEviction).evictTransactionHistory(userId);

        ArgumentCaptor<TransactionCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionCompletedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TransactionCompletedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(event.amount()).isEqualByComparingTo("25.00");
        assertThat(event.primaryEmail()).isEqualTo("plamen@example.com");
        assertThat(event.primaryUsername()).isEqualTo("Plamen");
        assertThat(event.secondaryEmail()).isNull();
    }

    @Test
    void deposit_walletNotFound_throwsAndSavesNothing() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        DepositMoneyDTO dto = new DepositMoneyDTO();
        dto.setAmount(new BigDecimal("25.00"));
        dto.setCardCvc("123");
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(WalletNotFoundException.class, () -> transactionService.deposit(userId, dto));

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- WITHDRAW ---

    @Test
    void withdraw_success_decreasesBalance_andChecksDailyLimit() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(bankCardRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        WithdrawMoneyDTO dto = new WithdrawMoneyDTO();
        dto.setAmount(new BigDecimal("40.00"));
        dto.setSpendingCategory(SpendingCategory.BILLS);

        transactionService.withdraw(userId, dto);

        assertThat(wallet.getBalance()).isEqualByComparingTo("60.00");
        verify(withdrawDailyLimitService).assertWithinDailyLimit(userId, new BigDecimal("40.00"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        ArgumentCaptor<TransactionCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionCompletedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("40.00");
    }

    @Test
    void withdraw_insufficientBalance_throwsBeforeCheckingDailyLimit() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        WithdrawMoneyDTO dto = new WithdrawMoneyDTO();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setSpendingCategory(SpendingCategory.BILLS);

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(userId, dto));
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");

        verify(withdrawDailyLimitService, never()).assertWithinDailyLimit(any(), any());
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void withdraw_exceedsDailyLimit_throwsAndLeavesBalanceUnchanged() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        doThrow(new DailyWithdrawLimitExceededException(BigDecimal.ZERO))
                .when(withdrawDailyLimitService).assertWithinDailyLimit(eq(userId), any());

        WithdrawMoneyDTO dto = new WithdrawMoneyDTO();
        dto.setAmount(new BigDecimal("40.00"));
        dto.setSpendingCategory(SpendingCategory.BILLS);

        assertThrows(DailyWithdrawLimitExceededException.class,
                () -> transactionService.withdraw(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // --- TRANSFER ---

    @Test
    void transfer_success_setsPendingStatus_andDeductsSenderImmediately() {
        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("10.00"));

        BankCard receiverCard = new BankCard();
        receiverCard.setLastFourDigits("4242");

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Georgi")).thenReturn(Optional.of(receiverUser));
        when(walletRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverWallet));
        when(bankCardRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverCard));

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Georgi");
        dto.setAmount(new BigDecimal("30.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        transactionService.transfer(userId, dto);

        assertThat(wallet.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("10.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.TRANSFER);

        verify(cacheEviction).evictTransactionHistoryForWallets(wallet, receiverWallet);
    }

    @Test
    void transfer_toSelf_throwsNotTransferMoneyYourselfException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(walletRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(wallet));

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Plamen");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(NotTransferMoneyYourselfException.class,
                () -> transactionService.transfer(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_receiverUserNotFound_throwsUserNotFoundException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Unknown")).thenReturn(Optional.empty());

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Unknown");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(UserNotFoundException.class, () -> transactionService.transfer(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transfer_receiverWalletNotFound_throwsReceiverNotFoundException() {
        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Georgi")).thenReturn(Optional.of(receiverUser));
        when(walletRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.empty());

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Georgi");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(ReceiverNotFoundException.class, () -> transactionService.transfer(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_success_persistsSenderWalletAndSetsDescriptionWithCardMask() {
        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("10.00"));

        BankCard receiverCard = new BankCard();
        receiverCard.setLastFourDigits("4242");

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Georgi")).thenReturn(Optional.of(receiverUser));
        when(walletRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverWallet));
        when(bankCardRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverCard));

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Georgi");
        dto.setAmount(new BigDecimal("30.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        transactionService.transfer(userId, dto);

        verify(walletRepository).save(wallet);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction saved = txCaptor.getValue();
        assertThat(saved.getDescription()).contains("****4242");
        assertThat(saved.getDescription()).startsWith("Shopping");
    }

    @Test
    void transfer_receiverWithoutBankCard_throwsReceiverBankCardNotFoundException() {
        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(BigDecimal.ZERO);

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Georgi")).thenReturn(Optional.of(receiverUser));
        when(walletRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverWallet));
        when(bankCardRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.empty());

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Georgi");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(ReceiverBankCardNotFoundException.class,
                () -> transactionService.transfer(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_insufficientBalance_throwsInsufficientBalanceException() {
        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiverUser);

        BankCard receiverCard = new BankCard();
        receiverCard.setLastFourDigits("4242");

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(userRepository.findByUsername("Georgi")).thenReturn(Optional.of(receiverUser));
        when(walletRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverWallet));
        when(bankCardRepository.findByUser_Id(receiverUser.getId())).thenReturn(Optional.of(receiverCard));

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Georgi");
        dto.setAmount(new BigDecimal("500.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.transfer(userId, dto));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    // --- WELCOME BONUS ---

    @Test
    void grantWelcomeBonus_addsFiftyEuros_andCreatesCompletedDepositTransaction() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        transactionService.grantWelcomeBonus(userId);
        assertThat(wallet.getBalance()).isEqualByComparingTo("150.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction saved = txCaptor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("50.00");
        assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getReceiverWallet()).isEqualTo(wallet);
        assertThat(saved.getSenderWallet()).isNull();
    }

    @Test
    void transfer_senderWalletNotFound_throwsSenderNotFoundException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        TransferMoneyDTO dto = new TransferMoneyDTO();
        dto.setReceiverUsername("Tony");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setSpendingCategory(SpendingCategory.SHOPPING);

        assertThrows(SenderNotFoundException.class, () -> transactionService.transfer(userId, dto));
        verify(transactionRepository, never()).save(any());
    }

    // --- CANCEL PENDING TRANSFER ---

    @Test
    void cancelPendingTransfer_success_delegatesToProcessingService() {
        UUID txId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setId(txId);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setSenderWallet(wallet);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));

        transactionService.cancelPendingTransfer(txId, userId);

        verify(pendingTransferProcessingService).cancelPendingTransfer(txId);
    }

    @Test
    void cancelPendingTransfer_notFound_throwsPendingTransferNotFoundException() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

        assertThrows(PendingTransferNotFoundException.class,
                () -> transactionService.cancelPendingTransfer(txId, userId));

        verify(pendingTransferProcessingService, never()).cancelPendingTransfer(any());
    }

    @Test
    void cancelPendingTransfer_alreadyCompleted_throwsCannotCancelTransferException() {
        UUID txId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setId(txId);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setSenderWallet(wallet);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));

        assertThrows(CannotCancelTransferException.class,
                () -> transactionService.cancelPendingTransfer(txId, userId));

        verify(pendingTransferProcessingService, never()).cancelPendingTransfer(any());
    }

    @Test
    void cancelPendingTransfer_notInitiator_throwsCannotCancelTransferException() {
        UUID txId = UUID.randomUUID();
        UUID someoneElseId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setId(txId);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setSenderWallet(wallet);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));

        assertThrows(CannotCancelTransferException.class,
                () -> transactionService.cancelPendingTransfer(txId, someoneElseId));

        verify(pendingTransferProcessingService, never()).cancelPendingTransfer(any());
    }

    // --- GET FILTERED USER TRANSACTIONS ---

    @Test
    void getFilteredUserTransactions_walletNotFound_throwsWalletNotFoundException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> transactionService.getFilteredUserTransactions(userId, new TransactionHistoryFilter()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredUserTransactions_mapsSenderAndReceiverDetailsCorrectly() {

        User receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setUsername("Georgi");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiverUser);

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setDescription("Shopping (to card ****4242)");
        transaction.setCreatedAt(LocalDateTime.of(2026, 7, 7, 12, 30, 0));
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setSenderWallet(wallet);
        transaction.setReceiverWallet(receiverWallet);

        UserProfileDetails receiverProfile = new UserProfileDetails();
        receiverProfile.setAccountStatus(AccountStatus.INACTIVE);

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(transaction));
        when(profileDetailsRepository.findByUser_Username("Plamen")).thenReturn(Optional.empty());
        when(profileDetailsRepository.findByUser_Username("Georgi")).thenReturn(Optional.of(receiverProfile));

        List<TransactionViewDTO> result =
                transactionService.getFilteredUserTransactions(userId, new TransactionHistoryFilter());

        assertThat(result).hasSize(1);

        TransactionViewDTO dto = result.getFirst();
        assertThat(dto.getAmount()).isEqualByComparingTo("15.00");
        assertThat(dto.getSenderUsername()).isEqualTo("Plamen");
        assertThat(dto.getSenderAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(dto.getReceiverUsername()).isEqualTo("Georgi");
        assertThat(dto.getReceiverAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(dto.getCreatedAt()).isEqualTo("2026-07-07 12:30:00");
    }

    // --- CLEAR Transaction History ---

    @Test
    void clearUserTransactionHistory_walletNotFound_throwsWalletNotFoundException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> transactionService.clearUserTransactionHistory(userId));

        verify(transactionRepository, never()).deleteAll(any());
    }

    @Test
    void clearUserTransactionHistory_refundsPendingOutgoingTransfers_thenDeletesAll() {

        Transaction pendingOutgoing = new Transaction();
        pendingOutgoing.setId(UUID.randomUUID());
        pendingOutgoing.setType(TransactionType.TRANSFER);
        pendingOutgoing.setStatus(TransactionStatus.PENDING);
        pendingOutgoing.setSenderWallet(wallet);

        Transaction completedDeposit = new Transaction();
        completedDeposit.setId(UUID.randomUUID());
        completedDeposit.setType(TransactionType.DEPOSIT);
        completedDeposit.setStatus(TransactionStatus.COMPLETED);
        completedDeposit.setReceiverWallet(wallet);

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findAllBySenderWalletOrReceiverWallet(wallet, wallet))
                .thenReturn(List.of(pendingOutgoing, completedDeposit));

        transactionService.clearUserTransactionHistory(userId);

        verify(transferRefundSupport).refundSenderAndSetStatus(pendingOutgoing, TransactionStatus.CANCELLED);
        verify(transferRefundSupport, never()).refundSenderAndSetStatus(completedDeposit, TransactionStatus.CANCELLED);
        verify(transactionRepository).deleteAll(List.of(pendingOutgoing, completedDeposit));
        verify(cacheEviction).evictTransactionHistory(userId);
    }

    @Test
    void clearUserTransactionHistory_noTransactions_skipsDelete() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findAllBySenderWalletOrReceiverWallet(wallet, wallet))
                .thenReturn(List.of());

        transactionService.clearUserTransactionHistory(userId);

        verify(transactionRepository, never()).deleteAll(anyList());
        verify(transferRefundSupport, never()).refundSenderAndSetStatus(any(), any());
    }
}