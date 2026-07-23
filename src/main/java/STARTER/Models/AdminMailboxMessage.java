package STARTER.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "admin_mailbox_messages")
public class AdminMailboxMessage extends BaseClass {

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "recipient_username", nullable = false)
    private String recipientUsername;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(name = "admin_recipient_username")
    private String adminRecipientUsername;

    @Column(name = "from_admin", nullable = false)
    @Builder.Default
    private boolean fromAdmin = true;

    @Column(nullable = false)
    private String body;

    @Column(name = "read_by_recipient", nullable = false)
    @Builder.Default
    private boolean readByRecipient = false;

    @Column(name = "read_by_admin", nullable = false)
    @Builder.Default
    private boolean readByAdmin = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    protected void onPrePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
