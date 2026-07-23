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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllBySenderWalletOrReceiverWallet(
        Wallet senderWallet,
        Wallet receiverWallet
    );

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

    @Query(
            """
                SELECT COUNT(t)
                FROM Transaction t
                WHERE t.senderWallet.id = :senderWalletId
                    AND t.type = STARTER.Enums.TransactionType.TRANSFER
                    AND t.createdAt >= :startOfDay
                    AND t.createdAt < :endOfDay
            """
    )
    long countTransfersBetween(
            @Param("senderWalletId") UUID senderWalletId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query(
            """
                SELECT COUNT(t) > 0
                FROM Transaction t
                WHERE t.senderWallet.id = :senderWalletId
                    AND t.receiverWallet.id = :receiverWalletId
                    AND t.type = STARTER.Enums.TransactionType.TRANSFER
            """
    )
    boolean existsTransferBetweenSenderAndReceiver(
            @Param("senderWalletId") UUID senderWalletId,
            @Param("receiverWalletId") UUID receiverWalletId
    );

    @Query(
            """
                SELECT COUNT(t) > 0
                FROM Transaction t
                WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
                    AND t.status IN :statuses
            """
    )
    boolean existsByWalletInvolvedAndStatusIn(
            @Param("walletId") UUID walletId,
            @Param("statuses") Collection<TransactionStatus> statuses
    );

    long countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            TransactionType type,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    long countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            TransactionType type,
            TransactionStatus status,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    long countByTypeAndStatus(TransactionType type, TransactionStatus status);
}
