package STARTER.Repositories;

import STARTER.Enums.MoneyRequestStatus;
import STARTER.Models.MoneyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, UUID> {

    List<MoneyRequest> findByRequester_IdOrderByCreatedAtDesc(UUID requesterId);

    List<MoneyRequest> findByPayer_IdOrderByCreatedAtDesc(UUID payerId);

    long countByPayer_UsernameAndStatus(String payerUsername, MoneyRequestStatus status);

    @Modifying(clearAutomatically = true)
    @Query("delete from MoneyRequest m where m.requester.id = :userId or m.payer.id = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);
}
