package STARTER.DTOs;

import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AdminUserViewDTO {

    private UUID id;
    private String username;
    private String email;
    private BigDecimal balance;
    private String currency;
    private String createdAt;
    private UserRole role;
    private String roleDisplay;
    private boolean primaryAdminAccount;
    private AccountStatus accountStatus;
    private String phone;
    private String avatarImageSrc;
}
