package STARTER.DTOs;

import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Pattern(regexp = ValidationPatterns.RESET_TOKEN, message = "Invalid reset token")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = ValidationPatterns.PASSWORD,
            message = "Password must include an uppercase letter, a lowercase letter, a digit, and a special character"
    )

    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String confirmPassword;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmMatching() {

        if (newPassword == null || confirmPassword == null) {
            return true;
        }

        return newPassword.equals(confirmPassword);
    }
}
