package STARTER.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SystemAnnouncementFormDTO {

    @NotBlank(message = "Announcement message is required.")
    @Size(max = 500, message = "Announcement must be at most 500 characters.")
    private String message;
}
