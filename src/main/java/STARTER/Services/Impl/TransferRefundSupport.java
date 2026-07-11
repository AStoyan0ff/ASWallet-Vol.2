package STARTER.Services.Impl;

import STARTER.Enums.TransactionStatus;
import STARTER.Models.Transaction;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.WalletRepository;
import org.springframework.stereotype.Component;

@Component
public class TransferRefundSupport {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransferRefundSupport(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public void refundSenderAndSetStatus(Transaction transaction, TransactionStatus status) {

        if (transaction.getStatus() != TransactionStatus.PENDING &&
            transaction.getStatus() != TransactionStatus.PENDING_RISK_REVIEW) {
            return;
        }

        Wallet senderWallet = transaction.getSenderWallet();
        senderWallet.setBalance(senderWallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(senderWallet);

        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }
}
