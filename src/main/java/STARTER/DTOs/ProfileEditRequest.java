package STARTER.DTOs;

import STARTER.Utils.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileEditRequest {

    @Size(max = 50, message = "First name must be at most 50 characters")
    @Pattern(
        regexp = ValidationPatterns.OPTIONAL_PERSON_NAME,
        message = "First name must start with a letter and contain only letters, spaces, hyphen, or apostrophe")

    private String firstName;

    @Size(max = 50, message = "Last name must be at most 50 characters")
    @Pattern(
        regexp = ValidationPatterns.OPTIONAL_PERSON_NAME,
        message = "Last name must start with a letter and contain only letters, spaces, hyphen, or apostrophe")

    private String lastName;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    @Pattern(
        regexp = ValidationPatterns.OPTIONAL_PHONE,
        message = "Phone number must contain 7 to 15 digits and may start with +")

    private String phoneNumber;
}
