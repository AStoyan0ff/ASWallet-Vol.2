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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingTransferProcessingServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ApplicationCacheEviction cacheEviction;
    @Mock private TransferRefundSupport transferRefundSupport;

    @InjectMocks
    private PendingTransferProcessingService pendingTransferProcessingService;

    private Transaction pendingTransfer;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pendingTransferProcessingService, "self", pendingTransferProcessingService);
        ReflectionTestUtils.setField(pendingTransferProcessingService, "processingDelaySeconds", 5);
        ReflectionTestUtils.setField(pendingTransferProcessingService, "failureRate", 0.0);
        ReflectionTestUtils.setField(pendingTransferProcessingService, "staleMinutes", 15);

        pendingTransfer = buildPendingTransfer(new BigDecimal("30.00"));
        senderWallet = pendingTransfer.getSenderWallet();
        receiverWallet = pendingTransfer.getReceiverWallet();
    }

    // --- FINALIZE ---

    @Test
    void finalizePendingTransfer_success_creditsReceiverAndCompletesTransaction() {
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.finalizePendingTransfer(pendingTransfer.getId());

        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("40.00");
        assertThat(pendingTransfer.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        verify(walletRepository).save(receiverWallet);
        verify(transactionRepository).save(pendingTransfer);
        verify(cacheEviction).evictTransactionHistoryForWallets(senderWallet, receiverWallet);
        verify(transferRefundSupport, never()).refundSenderAndSetStatus(any(), any());

        ArgumentCaptor<TransactionCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TransactionCompletedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(event.amount()).isEqualByComparingTo("30.00");
        assertThat(event.primaryUsername()).isEqualTo("Plamen");
        assertThat(event.secondaryUsername()).isEqualTo("Georgi");
    }

    @Test
    void finalizePendingTransfer_simulatedFailure_refundsSenderAndMarksFailed() {

        ReflectionTestUtils.setField(pendingTransferProcessingService, "failureRate", 1.0);
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.finalizePendingTransfer(pendingTransfer.getId());

        verify(transferRefundSupport).refundSenderAndSetStatus(pendingTransfer, TransactionStatus.FAILED);
        verify(cacheEviction).evictTransactionHistoryForWallets(senderWallet, receiverWallet);
        verify(walletRepository, never()).save(receiverWallet);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void finalizePendingTransfer_notPendingAnymore_doesNothing() {
        pendingTransfer.setStatus(TransactionStatus.COMPLETED);
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.finalizePendingTransfer(pendingTransfer.getId());

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(transferRefundSupport, never()).refundSenderAndSetStatus(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void finalizePendingTransfer_notFound_throwsPendingTransferNotFoundException() {

        UUID missingId = UUID.randomUUID();
        when(transactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(PendingTransferNotFoundException.class,
                () -> pendingTransferProcessingService.finalizePendingTransfer(missingId));
    }

    // --- CANCEL ---

    @Test
    void cancelPendingTransfer_success_refundsSenderAndMarksCancelled() {
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.cancelPendingTransfer(pendingTransfer.getId());

        verify(transferRefundSupport).refundSenderAndSetStatus(pendingTransfer, TransactionStatus.CANCELLED);
        verify(cacheEviction).evictTransactionHistoryForWallets(senderWallet, receiverWallet);
    }

    @Test
    void cancelPendingTransfer_notPendingAnymore_doesNothing() {
        pendingTransfer.setStatus(TransactionStatus.CANCELLED);
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.cancelPendingTransfer(pendingTransfer.getId());

        verify(transferRefundSupport, never()).refundSenderAndSetStatus(any(), any());
    }

    // --- BATCH PROCESSING ---

    @Test
    void processReadyPendingTransfers_finalizesEachReadyTransfer() {
        when(transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                eq(TransactionStatus.PENDING),
                eq(TransactionType.TRANSFER),
                any(LocalDateTime.class)
        )).thenReturn(List.of(pendingTransfer));
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.processReadyPendingTransfers();

        assertThat(pendingTransfer.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(walletRepository).save(receiverWallet);
    }

    @Test
    void processReadyPendingTransfers_emptyList_doesNothing() {
        when(transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                eq(TransactionStatus.PENDING),
                eq(TransactionType.TRANSFER),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        pendingTransferProcessingService.processReadyPendingTransfers();

        verify(transactionRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void processReadyPendingTransfers_swallowsRuntimeExceptionFromFinalize() {

        UUID missingId = UUID.randomUUID();
        Transaction other = buildPendingTransfer(new BigDecimal("10.00"));
        other.setId(missingId);

        when(transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                eq(TransactionStatus.PENDING),
                eq(TransactionType.TRANSFER),
                any(LocalDateTime.class)
        )).thenReturn(List.of(other));
        when(transactionRepository.findById(missingId)).thenReturn(Optional.empty());

        pendingTransferProcessingService.processReadyPendingTransfers();

        verify(transactionRepository).findById(missingId);
    }

    @Test
    void cancelStalePendingTransfers_cancelsEachStaleTransfer() {
        when(transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                eq(TransactionStatus.PENDING),
                eq(TransactionType.TRANSFER),
                any(LocalDateTime.class)
        )).thenReturn(List.of(pendingTransfer));
        when(transactionRepository.findById(pendingTransfer.getId())).thenReturn(Optional.of(pendingTransfer));

        pendingTransferProcessingService.cancelStalePendingTransfers();

        verify(transferRefundSupport).refundSenderAndSetStatus(pendingTransfer, TransactionStatus.CANCELLED);
    }

    @Test
    void cancelStalePendingTransfers_emptyList_doesNothing() {
        when(transactionRepository.findByStatusAndTypeAndCreatedAtBefore(
                eq(TransactionStatus.PENDING),
                eq(TransactionType.TRANSFER),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        pendingTransferProcessingService.cancelStalePendingTransfers();

        verify(transferRefundSupport, never()).refundSenderAndSetStatus(any(), any());
    }

    private Transaction buildPendingTransfer(BigDecimal amount) {

        User sender = new User();
        sender.setId(UUID.randomUUID());
        sender.setUsername("Plamen");
        sender.setEmail("plamen@example.com");

        User receiver = new User();
        receiver.setId(UUID.randomUUID());
        receiver.setUsername("Georgi");
        receiver.setEmail("georgi@example.com");

        senderWallet = new Wallet();
        senderWallet.setId(UUID.randomUUID());
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("70.00"));

        receiverWallet = new Wallet();
        receiverWallet.setId(UUID.randomUUID());
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("10.00"));

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(amount);
        transaction.setDescription("Shopping (to card ****4242)");
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);
        transaction.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        return transaction;
    }
}
