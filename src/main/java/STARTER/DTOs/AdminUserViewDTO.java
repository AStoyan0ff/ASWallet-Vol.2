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
// Advanced: admin user view with account status
public class AdminUserViewDTO {

    private UUID id;
    private String username;
    private String email;
    private BigDecimal balance;
    private String currency;
    private String createdAt;
    private UserRole role;
    // Advanced — display label: SUPER ADMIN / ADMIN / USER
    private String roleDisplay;
    private boolean primaryAdminAccount;
    private AccountStatus accountStatus;
    // Advanced — profile snapshot for admin manage view
    private String phone;
    private String avatarImageSrc;
}
