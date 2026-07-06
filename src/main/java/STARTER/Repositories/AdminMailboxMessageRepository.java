package STARTER.Repositories;

import STARTER.Models.AdminMailboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminMailboxMessageRepository extends JpaRepository<AdminMailboxMessage, UUID> {

    List<AdminMailboxMessage> findAllByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);
    List<AdminMailboxMessage> findAllByRecipientUsernameOrderByCreatedAtAsc(String recipientUsername);
    List<AdminMailboxMessage> findAllByFromAdminFalseOrderByCreatedAtDesc();

    Optional<AdminMailboxMessage> findByIdAndRecipientUsername(UUID id, String recipientUsername);

    long countByRecipientUsernameAndFromAdminTrueAndReadByRecipientFalse(String recipientUsername);
    long countByFromAdminFalseAndReadByAdminFalse();

    void deleteByRecipientUserId(UUID recipientUserId);
    void deleteByRecipientUsername(String recipientUsername);
}
