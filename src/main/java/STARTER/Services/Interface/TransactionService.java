package STARTER.Services.Interface;

import STARTER.DTOs.DepositMoneyDTO;
import STARTER.DTOs.TransactionHistoryFilter;
import STARTER.DTOs.TransactionViewDTO;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.DTOs.WithdrawMoneyDTO;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    void transfer(UUID senderUserId, TransferMoneyDTO transferMoneyDTO);
    void deposit(UUID userID, DepositMoneyDTO depositMoneyDTO);
    void withdraw(UUID userID, WithdrawMoneyDTO withdrawMoneyDTO);
    void grantWelcomeBonus(UUID userId);

    // Advanced — process pending transfers (scheduler)
    void processPendingTransfers();

    // Advanced — auto-cancel stale pending transfers (scheduler)
    void cancelStalePendingTransfers();

    // Advanced — user cancels own pending transfer
    void cancelPendingTransfer(UUID transactionId, UUID userId);

    List<TransactionViewDTO> getUserTransactions(UUID userID);

    // Advanced — filtered history shared by UI and exports
    List<TransactionViewDTO> getFilteredUserTransactions(UUID userId, TransactionHistoryFilter filter);

    // Advanced — clear all transactions involving the user's wallet
    void clearUserTransactionHistory(UUID userId);
}
