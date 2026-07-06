package STARTER.Services.Interface;

import STARTER.DTOs.AdminUserViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    List<AdminUserViewDTO> getAllUsers();
    void deleteUser(String adminUsername, UUID userId);

    // Advanced: admin account status management
    void updateAccountStatus(String adminUsername, UUID userId, AccountStatus newStatus);

    // Advanced — admin role management
    void updateUserRole(String adminUsername, UUID userId, UserRole newRole);

    // Advanced — single user manage page (status + role)
    AdminUserViewDTO getManageableUser(String adminUsername, UUID userId);
}
