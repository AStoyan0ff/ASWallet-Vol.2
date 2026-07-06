package STARTER.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Advanced — admin option for user send-message form
public class AdminRecipientOptionDTO {

    private String username;
    private String displayName;
}
