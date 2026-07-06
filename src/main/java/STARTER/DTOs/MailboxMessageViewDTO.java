package STARTER.DTOs;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Advanced — user mailbox message view
public class MailboxMessageViewDTO {

    private UUID id;
    private String senderUsername;
    private String senderDisplayName;
    private String adminRecipientUsername;
    private String adminRecipientDisplayName;
    private boolean fromAdmin;
    private String body;
    private String preview;
    private String createdAt;
    private boolean readByRecipient;
    private boolean readByAdmin;
}
