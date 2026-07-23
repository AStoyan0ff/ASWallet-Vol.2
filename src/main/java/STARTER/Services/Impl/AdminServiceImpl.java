package STARTER.Services.Impl;

import STARTER.CustomException.*;
import STARTER.DTOs.AdminUserViewDTO;
import STARTER.DTOs.UserProfileDetailsViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.AdminService;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Utils.DateTimeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserDeletionService userDeletionService;
    private final UserProfileDetailsService userProfileDetailsService;

    @Value("${app.admin.username:admin}")
    private String primaryAdminUsername;

    public AdminServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            UserDeletionService userDeletionService,
            UserProfileDetailsService userProfileDetailsService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.userDeletionService = userDeletionService;
        this.userProfileDetailsService = userProfileDetailsService;
    }

    @Override
    public List<AdminUserViewDTO> getAllUsers() {

        return userRepository
            .findAll()
            .stream()
            .sorted(Comparator.comparing(User::getCreatedAt)
            .reversed())
            .map(this::mapToAdminView)
            .toList();
    }

    @Override
    @Transactional
    public void deleteUser(String adminUsername, UUID userId) {
        User admin = findUser(adminUsername);
        User target = findUserById(userId);

        assertActorIsAdmin(admin);
        assertCanManageUser(admin, target);

        if (admin.getId().equals(target.getId())) {
            throw new CannotDeleteSelfException("You cannot delete your own account from the admin panel.");
        }

        if (isPrimaryAdminAccount(target)) {
            throw new CannotDeleteAdminException("The primary admin account cannot be deleted.");
        }

        if (target.getRole() == UserRole.ADMIN) {
            throw new CannotDeleteAdminException("Admin accounts cannot be deleted from the admin panel.");
        }

        userDeletionService.deleteUserFully(target);
        logger.info("Admin deleted user: admin={}, targetUsername={}, targetId={}",
                adminUsername, target.getUsername(), userId);
    }

    @Override
    public void updateAccountStatus(String adminUsername, UUID userId, AccountStatus newStatus) {
        userProfileDetailsService.updateAccountStatus(adminUsername, userId, newStatus);
    }

    @Override
    @Transactional
    public void updateUserRole(String adminUsername, UUID userId, UserRole newRole) {
        User admin = findUser(adminUsername);
        User target = findUserById(userId);

        if (newRole == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        assertActorIsAdmin(admin);
        assertCanManageUser(admin, target);
        assertPrimaryAdminCanChangeRoles(admin);

        target.setRole(newRole);
        userRepository.save(target);
        logger.info("Admin updated user role: admin={}, targetUsername={}, targetId={}, role={}",
                adminUsername, target.getUsername(), userId, newRole);
    }

    @Override
    public AdminUserViewDTO getManageableUser(String adminUsername, UUID userId) {
        User admin = findUser(adminUsername);
        User target = findUserById(userId);

        assertActorIsAdmin(admin);
        assertCanManageUser(admin, target);

        return mapToAdminView(target);
    }

    private void assertActorIsAdmin(User admin) {

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CannotChangeAdminRoleException(
                    "You do not have admin permissions. Please log out and log in again.");
        }
    }

    private void assertCanManageUser(User admin, User target) {

        if (admin.getId().equals(target.getId())) {
            throw new CannotChangeSelfRoleException("You cannot manage your own account here.");
        }

        if (isPrimaryAdminAccount(target)) {
            throw new CannotChangeAdminRoleException("The primary admin account cannot be managed.");
        }

        if (!isPrimaryAdmin(admin.getUsername()) && target.getRole() == UserRole.ADMIN) {
            throw new CannotChangeAdminRoleException("Only the super admin can manage other admin accounts.");
        }
    }

    private void assertPrimaryAdminCanChangeRoles(User admin) {

        if (!isPrimaryAdmin(admin.getUsername())) {
            throw new CannotChangeAdminRoleException("Only the super admin can change user roles.");
        }
    }

    private boolean isPrimaryAdmin(String username) {
        return primaryAdminUsername.trim().equals(username);
    }

    private boolean isPrimaryAdminAccount(User user) {
        return primaryAdminUsername.trim().equals(user.getUsername());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found"));
    }

    private AdminUserViewDTO mapToAdminView(User user) {

        Wallet wallet = walletRepository.findByUser_Id(user.getId()).orElse(null);
        UserProfileDetailsViewDTO profileView = userProfileDetailsService.getProfileView(user.getUsername());

        return AdminUserViewDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .balance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO)
                .currency(wallet != null ? wallet.getCurrency() : "EUR")
                .createdAt(DateTimeDisplay.format(user.getCreatedAt()))
                .role(user.getRole())
                .roleDisplay(resolveRoleDisplay(user))
                .primaryAdminAccount(isPrimaryAdminAccount(user))
                .accountStatus(profileView.getAccountStatus())
                .phone(profileView.getPhone())
                .avatarImageSrc(profileView.getAvatarImageSrc())
                .build();
    }

    private String resolveRoleDisplay(User user) {
        if (isPrimaryAdminAccount(user)) {
            return "SUPER ADMIN";
        }

        if (user.getRole() == UserRole.ADMIN) {
            return "SUPPORT ADMIN";
        }

        return user.getRole().name();
    }
}
