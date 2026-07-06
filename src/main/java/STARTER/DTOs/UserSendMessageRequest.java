package STARTER.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// Advanced — user send message to admin
public class UserSendMessageRequest {

    @NotBlank(message = "Select an admin to send your message to.")
    private String adminUsername;

    @NotBlank(message = "Message is required.")
    @Size(max = 2000, message = "Message must be at most 2000 characters.")
    private String message;
}
