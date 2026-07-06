package STARTER.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAccountRequest {

    @NotBlank(message = "Current password is required")
    @Size(min = 8, max = 64, message = "Password must be at most 64 characters")
    private String password;
}
