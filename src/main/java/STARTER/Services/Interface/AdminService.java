package STARTER.Services.Interface;

import STARTER.DTOs.AdminUserViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    List<AdminUserViewDTO> getAllUsers();

    void deleteUser(String adminUsername, UUID userId);
    void updateAccountStatus(String adminUsername, UUID userId, AccountStatus newStatus);
    void updateUserRole(String adminUsername, UUID userId, UserRole newRole);

    AdminUserViewDTO getManageableUser(String adminUsername, UUID userId);
}
