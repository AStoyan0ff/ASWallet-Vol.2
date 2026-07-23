package STARTER.Services.Interface;

import STARTER.DTOs.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    void transfer(UUID senderUserId, TransferMoneyDTO transferMoneyDTO);
    void deposit(UUID userID, DepositMoneyDTO depositMoneyDTO);
    void withdraw(UUID userID, WithdrawMoneyDTO withdrawMoneyDTO);
    void grantWelcomeBonus(UUID userId);
    void processPendingTransfers();
    void cancelStalePendingTransfers();
    void cancelPendingTransfer(UUID transactionId, UUID userId);

    List<TransactionViewDTO> getUserTransactions(UUID userID);
    Page<TransactionViewDTO> getUserTransactionsPage(UUID userId, int page, int size);

    boolean hasPendingTransfers(UUID userId);
    List<TransactionViewDTO> getFilteredUserTransactions(UUID userId, TransactionHistoryFilter filter);
    void clearUserTransactionHistory(UUID userId);
}
