package STARTER.Services.Impl;

import STARTER.CustomException.CannotChangeAdminRoleException;
import STARTER.CustomException.CannotChangeSelfRoleException;
import STARTER.CustomException.CannotDeleteAdminException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.AdminUserViewDTO;
import STARTER.DTOs.UserProfileDetailsViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Utils.DateTimeDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserDeletionService userDeletionService;
    @Mock private UserProfileDetailsService userProfileDetailsService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User primaryAdmin;
    private User supportAdmin;
    private User regularUser;
    private UUID primaryAdminId;
    private UUID supportAdminId;
    private UUID regularUserId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "primaryAdminUsername", "admin");

        primaryAdminId = UUID.randomUUID();
        supportAdminId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();

        primaryAdmin = buildUser(primaryAdminId, "admin", "admin@example.com", UserRole.ADMIN,
                LocalDateTime.of(2026, 1, 1, 10, 0));
        supportAdmin = buildUser(supportAdminId, "support", "support@example.com", UserRole.ADMIN,
                LocalDateTime.of(2026, 2, 1, 10, 0));
        regularUser = buildUser(regularUserId, "Plamen", "plamen@example.com", UserRole.USER,
                LocalDateTime.of(2026, 3, 1, 10, 0));
    }

    // --- GET ALL USERS ---

    @Test
    void getAllUsers_returnsUsersSortedByCreatedAtDescending() {
        when(userRepository.findAll()).thenReturn(List.of(regularUser, primaryAdmin, supportAdmin));
        stubProfileView(regularUser, AccountStatus.ACTIVE);
        stubProfileView(primaryAdmin, AccountStatus.ACTIVE);
        stubProfileView(supportAdmin, AccountStatus.INACTIVE);
        stubWallet(regularUser, new BigDecimal("100.00"), "EUR");
        when(walletRepository.findByUser_Id(primaryAdminId)).thenReturn(Optional.empty());
        when(walletRepository.findByUser_Id(supportAdminId)).thenReturn(Optional.empty());

        List<AdminUserViewDTO> result = adminService.getAllUsers();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getUsername()).isEqualTo("Plamen");
        assertThat(result.get(1).getUsername()).isEqualTo("support");
        assertThat(result.get(2).getUsername()).isEqualTo("admin");
    }

    @Test
    void getAllUsers_mapsRoleDisplayAndDefaultsWhenWalletMissing() {
        when(userRepository.findAll()).thenReturn(List.of(primaryAdmin, supportAdmin, regularUser));
        stubProfileView(primaryAdmin, AccountStatus.ACTIVE);
        stubProfileView(supportAdmin, AccountStatus.ACTIVE);
        stubProfileView(regularUser, AccountStatus.INACTIVE, "+359888", "/avatars/plamen.png");
        when(walletRepository.findByUser_Id(primaryAdminId)).thenReturn(Optional.empty());
        when(walletRepository.findByUser_Id(supportAdminId)).thenReturn(Optional.empty());
        stubWallet(regularUser, new BigDecimal("250.75"), "BGN");

        List<AdminUserViewDTO> result = adminService.getAllUsers();

        AdminUserViewDTO superAdminView = result.stream()
                .filter(dto -> "admin".equals(dto.getUsername()))
                .findFirst()
                .orElseThrow();
        AdminUserViewDTO supportAdminView = result.stream()
                .filter(dto -> "support".equals(dto.getUsername()))
                .findFirst()
                .orElseThrow();
        AdminUserViewDTO userView = result.stream()
                .filter(dto -> "Plamen".equals(dto.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(superAdminView.getRoleDisplay()).isEqualTo("SUPER ADMIN");
        assertThat(superAdminView.isPrimaryAdminAccount()).isTrue();
        assertThat(superAdminView.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(superAdminView.getCurrency()).isEqualTo("EUR");

        assertThat(supportAdminView.getRoleDisplay()).isEqualTo("SUPPORT ADMIN");
        assertThat(supportAdminView.isPrimaryAdminAccount()).isFalse();

        assertThat(userView.getRoleDisplay()).isEqualTo("USER");
        assertThat(userView.getBalance()).isEqualByComparingTo("250.75");
        assertThat(userView.getCurrency()).isEqualTo("BGN");
        assertThat(userView.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(userView.getPhone()).isEqualTo("+359888");
        assertThat(userView.getAvatarImageSrc()).isEqualTo("/avatars/plamen.png");
        assertThat(userView.getCreatedAt()).isEqualTo(DateTimeDisplay.format(regularUser.getCreatedAt()));
    }

    // --- DELETE USER ---

    @Test
    void deleteUser_primaryAdminDeletesRegularUser_callsDeletionService() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        adminService.deleteUser("admin", regularUserId);

        verify(userDeletionService).deleteUserFully(regularUser);
    }

    @Test
    void deleteUser_nonAdmin_throwsCannotChangeAdminRoleException() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(regularUser));
        when(userRepository.findById(supportAdminId)).thenReturn(Optional.of(supportAdmin));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminService.deleteUser("Plamen", supportAdminId));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    @Test
    void deleteUser_selfTarget_throwsCannotChangeSelfRoleException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));

        assertThrows(CannotChangeSelfRoleException.class,
                () -> adminService.deleteUser("admin", primaryAdminId));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    @Test
    void deleteUser_primaryAdminAccount_throwsCannotChangeAdminRoleException() {
        User anotherAdmin = buildUser(UUID.randomUUID(), "other-admin", "other@example.com", UserRole.ADMIN,
                LocalDateTime.of(2026, 4, 1, 10, 0));

        when(userRepository.findByUsername("other-admin")).thenReturn(Optional.of(anotherAdmin));
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminService.deleteUser("other-admin", primaryAdminId));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    @Test
    void deleteUser_supportAdminTarget_throwsCannotDeleteAdminException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(supportAdminId)).thenReturn(Optional.of(supportAdmin));

        assertThrows(CannotDeleteAdminException.class,
                () -> adminService.deleteUser("admin", supportAdminId));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    @Test
    void deleteUser_supportAdminManagingAnotherAdmin_throwsCannotChangeAdminRoleException() {
        User anotherSupportAdmin = buildUser(UUID.randomUUID(), "support2", "support2@example.com",
                UserRole.ADMIN, LocalDateTime.of(2026, 5, 1, 10, 0));

        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));
        when(userRepository.findById(anotherSupportAdmin.getId())).thenReturn(Optional.of(anotherSupportAdmin));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminService.deleteUser("support", anotherSupportAdmin.getId()));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    @Test
    void deleteUser_unknownAdmin_throwsUserNotFoundException() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> adminService.deleteUser("missing", regularUserId));

        verify(userDeletionService, never()).deleteUserFully(any());
    }

    // --- UPDATE ACCOUNT STATUS ---

    @Test
    void updateAccountStatus_delegatesToProfileDetailsService() {
        adminService.updateAccountStatus("admin", regularUserId, AccountStatus.INACTIVE);

        verify(userProfileDetailsService).updateAccountStatus("admin", regularUserId, AccountStatus.INACTIVE);
    }

    // --- UPDATE USER ROLE ---

    @Test
    void updateUserRole_primaryAdminPromotesUser_savesNewRole() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        adminService.updateUserRole("admin", regularUserId, UserRole.ADMIN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void updateUserRole_nullRole_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        assertThrows(IllegalArgumentException.class,
                () -> adminService.updateUserRole("admin", regularUserId, null));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_supportAdmin_throwsCannotChangeAdminRoleException() {
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminService.updateUserRole("support", regularUserId, UserRole.ADMIN));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_selfTarget_throwsCannotChangeSelfRoleException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));

        assertThrows(CannotChangeSelfRoleException.class,
                () -> adminService.updateUserRole("admin", primaryAdminId, UserRole.USER));

        verify(userRepository, never()).save(any());
    }

    // --- GET MANAGEABLE USER ---

    @Test
    void getManageableUser_primaryAdminViewingRegularUser_returnsMappedDto() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        stubProfileView(regularUser, AccountStatus.ACTIVE, "+359888", "/avatars/plamen.png");
        stubWallet(regularUser, new BigDecimal("75.00"), "EUR");

        AdminUserViewDTO result = adminService.getManageableUser("admin", regularUserId);

        assertThat(result.getId()).isEqualTo(regularUserId);
        assertThat(result.getUsername()).isEqualTo("Plamen");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.getRoleDisplay()).isEqualTo("USER");
        assertThat(result.getBalance()).isEqualByComparingTo("75.00");
        assertThat(result.getPhone()).isEqualTo("+359888");
    }

    @Test
    void getManageableUser_supportAdminManagingAnotherAdmin_throwsCannotChangeAdminRoleException() {
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));

        User anotherSupportAdmin = buildUser(UUID.randomUUID(), "support2", "support2@example.com",
                UserRole.ADMIN, LocalDateTime.of(2026, 5, 1, 10, 0));
        when(userRepository.findById(anotherSupportAdmin.getId())).thenReturn(Optional.of(anotherSupportAdmin));

        assertThrows(CannotChangeAdminRoleException.class,
                () -> adminService.getManageableUser("support", anotherSupportAdmin.getId()));
    }

    @Test
    void getManageableUser_unknownTarget_throwsUserNotFoundException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> adminService.getManageableUser("admin", regularUserId));
    }

    private User buildUser(UUID id, String username, String email, UserRole role, LocalDateTime createdAt) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("encoded")
                .role(role)
                .build();
        user.setId(id);
        user.setCreatedAt(createdAt);
        return user;
    }

    private void stubProfileView(User user, AccountStatus status) {
        stubProfileView(user, status, null, null);
    }

    private void stubProfileView(User user, AccountStatus status, String phone, String avatarImageSrc) {
        UserProfileDetailsViewDTO profileView = new UserProfileDetailsViewDTO();
        profileView.setUsername(user.getUsername());
        profileView.setAccountStatus(status);
        profileView.setPhone(phone);
        profileView.setAvatarImageSrc(avatarImageSrc);
        when(userProfileDetailsService.getProfileView(user.getUsername())).thenReturn(profileView);
    }

    private void stubWallet(User user, BigDecimal balance, String currency) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(balance);
        wallet.setCurrency(currency);
        when(walletRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(wallet));
    }
}
