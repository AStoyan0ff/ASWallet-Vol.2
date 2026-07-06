package STARTER.DTOs;

import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// Advanced — admin send mailbox message form
public class AdminSendMessageRequest {

    @NotBlank(message = "Username is required.")
    @Pattern(regexp = ValidationPatterns.USERNAME, message = "Invalid username format.")
    private String username;

    @NotBlank(message = "Message is required.")
    @Size(max = 2000, message = "Message must be at most 2000 characters.")
    private String message;
}
