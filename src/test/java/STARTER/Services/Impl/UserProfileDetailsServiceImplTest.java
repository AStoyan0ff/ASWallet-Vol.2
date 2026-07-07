package STARTER.Services.Impl;

import STARTER.CustomException.CannotChangeAdminAccountStatusException;
import STARTER.CustomException.CannotChangeSelfAccountStatusException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.DailyLimitEditRequest;
import STARTER.DTOs.ProfileEditRequest;
import STARTER.DTOs.UserProfileDetailsViewDTO;
import STARTER.DTOs.WalletSettingsRequest;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionType;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.AvatarStorageService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileDetailsRepository profileDetailsRepository;
    @Mock private AvatarStorageService avatarStorageService;
    @Mock private ApplicationCacheEviction cacheEviction;
    @Mock private WithdrawDailyLimitService withdrawDailyLimitService;

    @InjectMocks
    private UserProfileDetailsServiceImpl profileDetailsService;

    private User user;
    private User admin;
    private User supportAdmin;
    private UserProfileDetails profile;
    private UUID userId;
    private UUID adminId;
    private UUID supportAdminId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileDetailsService, "defaultAvatarImage", "/images/default-avatar.svg");
        ReflectionTestUtils.setField(profileDetailsService, "primaryAdminUsername", "admin");

        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        supportAdminId = UUID.randomUUID();

        user = buildUser(userId, "Plamen", "plamen@example.com", UserRole.USER);
        admin = buildUser(adminId, "admin", "admin@example.com", UserRole.ADMIN);
        supportAdmin = buildUser(supportAdminId, "support", "support@example.com", UserRole.ADMIN);

        profile = UserProfileDetails.builder()
                .user(user)
                .firstName("Plamen")
                .lastName("Ivanov")
                .phone("+359888")
                .avatarUrl("uploads/avatars/plamen.png")
                .accountStatus(AccountStatus.ACTIVE)
                .balanceHidden(false)
                .emailOnDeposit(true)
                .emailOnWithdraw(true)
                .emailOnTransfer(false)
                .dailyWithdrawLimit(new BigDecimal("200.00"))
                .build();
        profile.setId(UUID.randomUUID());
    }

    // --- CREATE DEFAULT FOR USERS ---

    @Test
    void createDefaultForUser_success_savesProfileWithDefaults() {
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(withdrawDailyLimitService.defaultDailyLimit()).thenReturn(new BigDecimal("500.00"));

        profileDetailsService.createDefaultForUser(user);

        ArgumentCaptor<UserProfileDetails> captor = ArgumentCaptor.forClass(UserProfileDetails.class);
        verify(profileDetailsRepository).save(captor.capture());

        UserProfileDetails saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isBalanceHidden()).isFalse();
        assertThat(saved.isEmailOnDeposit()).isTrue();
        assertThat(saved.getDailyWithdrawLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void createDefaultForUser_adminUser_savesWithoutDailyLimit() {
        when(profileDetailsRepository.findByUser_Id(adminId)).thenReturn(Optional.empty());

        profileDetailsService.createDefaultForUser(admin);

        ArgumentCaptor<UserProfileDetails> captor = ArgumentCaptor.forClass(UserProfileDetails.class);
        verify(profileDetailsRepository).save(captor.capture());
        assertThat(captor.getValue().getDailyWithdrawLimit()).isNull();
        verify(withdrawDailyLimitService, never()).defaultDailyLimit();
    }

    @Test
    void createDefaultForUser_whenProfileExists_doesNothing() {
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        profileDetailsService.createDefaultForUser(user);

        verify(profileDetailsRepository, never()).save(any());
    }

    @Test
    void ensureProfileExistsForAllUsers_createsMissingProfiles() {
        User anotherUser = buildUser(UUID.randomUUID(), "Georgi", "georgi@example.com", UserRole.USER);

        when(userRepository.findAll()).thenReturn(List.of(user, anotherUser));
        when(profileDetailsRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(profile));
        when(profileDetailsRepository.findByUser_Id(anotherUser.getId())).thenReturn(Optional.empty());
        when(withdrawDailyLimitService.defaultDailyLimit()).thenReturn(new BigDecimal("500.00"));

        profileDetailsService.ensureProfileExistsForAllUsers();

        verify(profileDetailsRepository).save(any(UserProfileDetails.class));
    }

    // --- GET PROFILE VIEW ---

    @Test
    void getProfileView_mapsFieldsAndResolvesRelativeAvatar() {
        stubUserAndProfile(user, profile);

        UserProfileDetailsViewDTO view = profileDetailsService.getProfileView("Plamen");

        assertThat(view.getId()).isEqualTo(userId);
        assertThat(view.getUsername()).isEqualTo("Plamen");
        assertThat(view.getEmail()).isEqualTo("plamen@example.com");
        assertThat(view.getFirstName()).isEqualTo("Plamen");
        assertThat(view.getPhone()).isEqualTo("+359888");
        assertThat(view.getAvatarImageSrc()).isEqualTo("/uploads/avatars/plamen.png");
        assertThat(view.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getProfileView_withoutAvatar_usesDefaultImage() {
        profile.setAvatarUrl(null);
        stubUserAndProfile(user, profile);

        UserProfileDetailsViewDTO view = profileDetailsService.getProfileView("Plamen");

        assertThat(view.getAvatarImageSrc()).isEqualTo("/images/default-avatar.svg");
    }

    @Test
    void getProfileView_withHttpAvatar_keepsAbsoluteUrl() {
        profile.setAvatarUrl("https://cdn.example.com/avatar.png");
        stubUserAndProfile(user, profile);

        UserProfileDetailsViewDTO view = profileDetailsService.getProfileView("Plamen");

        assertThat(view.getAvatarImageSrc()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void getProfileView_userNotFound_throws() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> profileDetailsService.getProfileView("missing"));
    }

    // --- BUILD EDIT REQUEST / UPDATE PROFILE ---

    @Test
    void buildEditRequest_returnsExistingValues() {
        stubUserAndProfile(user, profile);

        ProfileEditRequest request = profileDetailsService.buildEditRequest("Plamen");

        assertThat(request.getFirstName()).isEqualTo("Plamen");
        assertThat(request.getLastName()).isEqualTo("Ivanov");
        assertThat(request.getPhoneNumber()).isEqualTo("+359888");
    }

    @Test
    void updateProfile_updatesTextFieldsWithoutAvatar() {
        stubUserAndProfile(user, profile);

        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("  New ");
        request.setLastName("");
        request.setPhoneNumber("  ");

        profileDetailsService.updateProfile("Plamen", request, null);

        assertThat(profile.getFirstName()).isEqualTo("New");
        assertThat(profile.getLastName()).isNull();
        assertThat(profile.getPhone()).isNull();
        verify(profileDetailsRepository).save(profile);
        verify(avatarStorageService, never()).store(any(), any());
    }

    @Test
    void updateProfile_withAvatar_replacesStoredFile() {
        stubUserAndProfile(user, profile);

        MultipartFile avatarFile = mock(MultipartFile.class);
        when(avatarFile.isEmpty()).thenReturn(false);
        when(avatarStorageService.store(userId, avatarFile)).thenReturn("uploads/avatars/new.png");

        ProfileEditRequest request = new ProfileEditRequest();

        profileDetailsService.updateProfile("Plamen", request, avatarFile);

        verify(avatarStorageService).deleteLocalAvatar("uploads/avatars/plamen.png");
        verify(avatarStorageService).store(userId, avatarFile);
        assertThat(profile.getAvatarUrl()).isEqualTo("uploads/avatars/new.png");
        verify(profileDetailsRepository).save(profile);
    }

    // --- GET ACCOUNT STATUS ---

    @Test
    void getAccountStatus_returnsProfileStatus() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        assertThat(profileDetailsService.getAccountStatus("Plamen")).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getAccountStatus_whenProfileMissing_returnsActive() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThat(profileDetailsService.getAccountStatus("Plamen")).isEqualTo(AccountStatus.ACTIVE);
    }

    // --- UPDATE ACCOUNT STATUS ---

    @Test
    void updateAccountStatus_primaryAdminUpdatesUser_savesStatusAndEvictsCache() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        profileDetailsService.updateAccountStatus("admin", userId, AccountStatus.INACTIVE);

        assertThat(profile.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(profileDetailsRepository).save(profile);
        verify(cacheEviction).evictProfile("Plamen");
    }

    @Test
    void updateAccountStatus_nonAdmin_throws() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));

        assertThrows(CannotChangeAdminAccountStatusException.class,
                () -> profileDetailsService.updateAccountStatus("Plamen", supportAdminId, AccountStatus.INACTIVE));

        verify(profileDetailsRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_selfTarget_throws() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(CannotChangeSelfAccountStatusException.class,
                () -> profileDetailsService.updateAccountStatus("admin", adminId, AccountStatus.INACTIVE));
    }

    @Test
    void updateAccountStatus_supportAdminChangingAdmin_throws() {
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(supportAdmin));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(CannotChangeAdminAccountStatusException.class,
                () -> profileDetailsService.updateAccountStatus("support", adminId, AccountStatus.INACTIVE));
    }

    // --- WALLET SETTINGS ---

    @Test
    void buildWalletSettingsRequest_mapsProfileFlags() {
        stubUserAndProfile(user, profile);

        WalletSettingsRequest request = profileDetailsService.buildWalletSettingsRequest("Plamen");

        assertThat(request.isBalanceHidden()).isFalse();
        assertThat(request.isEmailOnDeposit()).isTrue();
        assertThat(request.isEmailOnWithdraw()).isTrue();
        assertThat(request.isEmailOnTransfer()).isFalse();
    }

    @Test
    void updateWalletSettings_savesFlags() {
        stubUserAndProfile(user, profile);

        WalletSettingsRequest request = new WalletSettingsRequest();
        request.setBalanceHidden(true);
        request.setEmailOnDeposit(false);
        request.setEmailOnWithdraw(false);
        request.setEmailOnTransfer(true);

        profileDetailsService.updateWalletSettings("Plamen", request);

        assertThat(profile.isBalanceHidden()).isTrue();
        assertThat(profile.isEmailOnDeposit()).isFalse();
        assertThat(profile.isEmailOnWithdraw()).isFalse();
        assertThat(profile.isEmailOnTransfer()).isTrue();
        verify(profileDetailsRepository).save(profile);
    }

    // --- DAILY LIMIT ---

    @Test
    void buildDailyLimitEditRequest_usesProfileLimit() {
        stubUserAndProfile(user, profile);

        DailyLimitEditRequest request = profileDetailsService.buildDailyLimitEditRequest("Plamen");

        assertThat(request.getDailyWithdrawLimit()).isEqualByComparingTo("200.00");
    }

    @Test
    void buildDailyLimitEditRequest_usesDefaultWhenProfileLimitMissing() {
        profile.setDailyWithdrawLimit(null);
        stubUserAndProfile(user, profile);
        when(withdrawDailyLimitService.defaultDailyLimit()).thenReturn(new BigDecimal("500.00"));

        DailyLimitEditRequest request = profileDetailsService.buildDailyLimitEditRequest("Plamen");

        assertThat(request.getDailyWithdrawLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void updateDailyLimit_savesNormalizedLimit() {
        stubUserAndProfile(user, profile);
        when(withdrawDailyLimitService.normalizeUserDailyLimit(new BigDecimal("150")))
                .thenReturn(new BigDecimal("150.00"));

        profileDetailsService.updateDailyLimit("Plamen", new BigDecimal("150"));

        assertThat(profile.getDailyWithdrawLimit()).isEqualByComparingTo("150.00");
        verify(profileDetailsRepository).save(profile);
    }

    @Test
    void updateDailyLimit_adminUser_skipsUpdate() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        profileDetailsService.updateDailyLimit("admin", new BigDecimal("150"));

        verify(profileDetailsRepository, never()).save(any());
        verify(withdrawDailyLimitService, never()).normalizeUserDailyLimit(any());
    }

    // --- HELPERS ---

    @Test
    void isBalanceHidden_returnsProfileValue() {
        profile.setBalanceHidden(true);
        stubUserAndProfile(user, profile);

        assertThat(profileDetailsService.isBalanceHidden("Plamen")).isTrue();
    }

    @Test
    void isTransactionEmailEnabled_returnsTypeSpecificFlag() {
        stubUserAndProfile(user, profile);

        assertThat(profileDetailsService.isTransactionEmailEnabled("Plamen", TransactionType.DEPOSIT)).isTrue();
        assertThat(profileDetailsService.isTransactionEmailEnabled("Plamen", TransactionType.WITHDRAW)).isTrue();
        assertThat(profileDetailsService.isTransactionEmailEnabled("Plamen", TransactionType.TRANSFER)).isFalse();
    }

    private User buildUser(UUID id, String username, String email, UserRole role) {
        User built = User.builder()
                .username(username)
                .email(email)
                .password("encoded")
                .role(role)
                .build();
        built.setId(id);
        return built;
    }

    private void stubUserAndProfile(User targetUser, UserProfileDetails targetProfile) {
        when(userRepository.findByUsername(targetUser.getUsername())).thenReturn(Optional.of(targetUser));
        when(profileDetailsRepository.findByUser_Id(targetUser.getId())).thenReturn(Optional.of(targetProfile));
    }
}
