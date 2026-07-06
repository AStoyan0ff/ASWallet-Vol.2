package STARTER.Repositories;

import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Models.Transaction;
import STARTER.Models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    // List<Transaction> findAllBySenderWallet(Wallet senderWallet);
    // List<Transaction> findAllByReceiverWallet(Wallet receiverWallet);

    List<Transaction> findAllBySenderWalletOrReceiverWallet(
        Wallet senderWallet,
        Wallet receiverWallet
    );

    // Advanced — pending transfer processing
    List<Transaction> findByStatusAndTypeAndCreatedAtBefore(
            TransactionStatus status,
            TransactionType type,
            LocalDateTime createdAt
    );
    // List<Transaction> findBySenderWalletIdAndTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan...()
    @Query(
            """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM Transaction t
                WHERE t.senderWallet.id = :walletId
                    AND t.type = STARTER.Enums.TransactionType.WITHDRAW
                    AND t.status = STARTER.Enums.TransactionStatus.COMPLETED
                    AND t.createdAt >= :startOfDay
                    AND t.createdAt < :endOfDay
            """
    )
    BigDecimal sumCompletedWithdrawalsBetween(
            @Param("walletId") UUID walletId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
