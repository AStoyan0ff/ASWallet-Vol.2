package STARTER.DTOs;

import STARTER.Enums.AccountStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileDetailsViewDTO {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String avatarImageSrc;
    private AccountStatus accountStatus;
}
