package STARTER.DTOs;

import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = ValidationPatterns.EMAIL,
            message = "Email must be in format name@domain.com"
    )
    private String email;
}
