package STARTER.Services.Impl;

import STARTER.Configuration.CacheConfig;
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
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UserProfileDetailsServiceImpl implements UserProfileDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileDetailsServiceImpl.class);

    private final UserRepository userRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final AvatarStorageService avatarStorageService;
    private final ApplicationCacheEviction cacheEviction;
    private final WithdrawDailyLimitService withdrawDailyLimitService;

    @Value("${app.avatar.default-image:/images/default-avatar.svg}")
    private String defaultAvatarImage;

    @Value("${app.admin.username:admin}")
    private String primaryAdminUsername;

    public UserProfileDetailsServiceImpl(
            UserRepository userRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            AvatarStorageService avatarStorageService,
            ApplicationCacheEviction cacheEviction,
            WithdrawDailyLimitService withdrawDailyLimitService) {

        this.userRepository = userRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.avatarStorageService = avatarStorageService;
        this.cacheEviction = cacheEviction;
        this.withdrawDailyLimitService = withdrawDailyLimitService;
    }

    @Override
    @Transactional
    public void createDefaultForUser(User user) {

        if (profileDetailsRepository.findByUser_Id(user.getId()).isPresent()) {
            return;
        }

        UserProfileDetails profile = UserProfileDetails.builder()
                .user(user)
                .accountStatus(AccountStatus.ACTIVE)
                .balanceHidden(false)
                .emailOnDeposit(true)
                .emailOnWithdraw(true)
                .emailOnTransfer(true)
                .dailyWithdrawLimit(user.getRole() == UserRole.ADMIN
                        ? null
                        : withdrawDailyLimitService.defaultDailyLimit())
                .build();

        profileDetailsRepository.save(profile);
    }

    @Override
    @Transactional
    public void ensureProfileExistsForAllUsers() {

        for (User user : userRepository.findAll()) {
            createDefaultForUser(user);
        }
    }

    @Override
    @Cacheable(value = CacheConfig.PROFILES, key = "#username")
    public UserProfileDetailsViewDTO getProfileView(String username) {

        User user = findUser(username);
        UserProfileDetails profile = findProfile(user.getId());

        return mapToView(user, profile);
    }

    @Override
    public ProfileEditRequest buildEditRequest(String username) {
        UserProfileDetails profile = findProfile(findUser(username).getId());

        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName(profile.getFirstName());
        request.setLastName(profile.getLastName());
        request.setPhoneNumber(profile.getPhone());

        return request;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.PROFILES, key = "#username")
    public void updateProfile(String username, ProfileEditRequest request, MultipartFile avatarFile) {

        User user = findUser(username);
        UserProfileDetails profile = findProfile(user.getId());

        profile.setFirstName(normalizeOptionalText(request.getFirstName()));
        profile.setLastName(normalizeOptionalText(request.getLastName()));
        profile.setPhone(normalizeOptionalText(request.getPhoneNumber()));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarStorageService.deleteLocalAvatar(profile.getAvatarUrl());
            profile.setAvatarUrl(avatarStorageService.store(user.getId(), avatarFile));
        }

        profileDetailsRepository.save(profile);
    }

    @Override
    public AccountStatus getAccountStatus(String username) {
        User user = findUser(username);

        return profileDetailsRepository.findByUser_Id(user.getId())
                .map(UserProfileDetails::getAccountStatus)
                .orElse(AccountStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void updateAccountStatus(String adminUsername, UUID userId, AccountStatus newStatus) {
        User admin = findUser(adminUsername);

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CannotChangeAdminAccountStatusException(
                    "You do not have admin permissions. Please log out and log in again.");
        }

        User userTarget = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (admin.getId().equals(userTarget.getId())) {
            throw new CannotChangeSelfAccountStatusException("You cannot change your own account status.");
        }

        if (userTarget.getRole() == UserRole.ADMIN) {

            if (!primaryAdminUsername.trim().equals(adminUsername)) {
                throw new CannotChangeAdminAccountStatusException(
                        "Only the primary admin can change admin account status.");
            }

            if (primaryAdminUsername.trim().equals(userTarget.getUsername())) {
                throw new CannotChangeAdminAccountStatusException(
                        "The primary admin account status cannot be changed.");
            }
        }

        UserProfileDetails profile = findProfile(userTarget.getId());

        profile.setAccountStatus(newStatus);
        profileDetailsRepository.save(profile);
        cacheEviction.evictProfile(userTarget.getUsername());

        logger.info("Admin updated account status: admin={}, targetUsername={}, targetId={}, status={}",
                adminUsername, userTarget.getUsername(), userId, newStatus);
    }

    @Override
    @Cacheable(value = CacheConfig.WALLET_SETTINGS, key = "#username")
    public WalletSettingsRequest buildWalletSettingsRequest(String username) {

        UserProfileDetails profile = findProfile(findUser(username).getId());
        WalletSettingsRequest request = new WalletSettingsRequest();

        request.setBalanceHidden(profile.isBalanceHidden());
        request.setEmailOnDeposit(profile.isEmailOnDeposit());
        request.setEmailOnWithdraw(profile.isEmailOnWithdraw());
        request.setEmailOnTransfer(profile.isEmailOnTransfer());

        return request;
    }

    @Override
    public DailyLimitEditRequest buildDailyLimitEditRequest(String username) {

        User user = findUser(username);
        UserProfileDetails profile = findProfile(user.getId());
        DailyLimitEditRequest request = new DailyLimitEditRequest();

        request.setDailyWithdrawLimit(profile.getDailyWithdrawLimit() != null
                ? profile.getDailyWithdrawLimit()
                : withdrawDailyLimitService.defaultDailyLimit());

        return request;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.WALLET_SETTINGS, key = "#username")
    public void updateDailyLimit(String username, BigDecimal dailyWithdrawLimit) {
        User user = findUser(username);

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        UserProfileDetails profile = findProfile(user.getId());
        profile.setDailyWithdrawLimit(
                withdrawDailyLimitService.normalizeUserDailyLimit(dailyWithdrawLimit)
        );

        profileDetailsRepository.save(profile);

        logger.info("Daily withdraw limit updated: username={}, dailyWithdrawLimit={}",
                username, profile.getDailyWithdrawLimit());
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.WALLET_SETTINGS, key = "#username")
    public void updateWalletSettings(String username, WalletSettingsRequest request) {

        User user = findUser(username);
        UserProfileDetails profile = findProfile(user.getId());

        profile.setBalanceHidden(request.isBalanceHidden());
        profile.setEmailOnDeposit(request.isEmailOnDeposit());
        profile.setEmailOnWithdraw(request.isEmailOnWithdraw());
        profile.setEmailOnTransfer(request.isEmailOnTransfer());

        profileDetailsRepository.save(profile);

        logger.info("Wallet settings updated: username={}, balanceHidden={}, emailOnDeposit={}, emailOnWithdraw={}, emailOnTransfer={}",
                username,
                request.isBalanceHidden(),
                request.isEmailOnDeposit(),
                request.isEmailOnWithdraw(),
                request.isEmailOnTransfer()
        );
    }

    @Override
    public boolean isBalanceHidden(String username) {
        return findProfile(findUser(username).getId()).isBalanceHidden();
    }

    @Override
    public boolean isTransactionEmailEnabled(String username, TransactionType type) {
        UserProfileDetails profile = findProfile(findUser(username).getId());

        return switch (type) {
            case DEPOSIT -> profile.isEmailOnDeposit();
            case WITHDRAW -> profile.isEmailOnWithdraw();
            case TRANSFER -> profile.isEmailOnTransfer();
        };
    }

    private User findUser(String username) {

        return userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));
    }

    private UserProfileDetails findProfile(UUID userId) {

        return profileDetailsRepository.findByUser_Id(userId).orElseThrow(() ->
                new UserNotFoundException("Profile not found"));
    }

    private UserProfileDetailsViewDTO mapToView(User user, UserProfileDetails profile) {
        UserProfileDetailsViewDTO details = new UserProfileDetailsViewDTO();

        details.setId(user.getId());
        details.setUsername(user.getUsername());
        details.setFirstName(profile.getFirstName());
        details.setLastName(profile.getLastName());
        details.setPhone(profile.getPhone());
        details.setEmail(user.getEmail());
        details.setAvatarUrl(profile.getAvatarUrl());
        details.setAvatarImageSrc(resolveAvatarImageSrc(profile.getAvatarUrl()));
        details.setAccountStatus(profile.getAccountStatus());

        return details;
    }

    private String resolveAvatarImageSrc(String avatarUrl) {

        if (avatarUrl == null || avatarUrl.isBlank()) {
            return defaultAvatarImage;
        }

        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            return avatarUrl;
        }

        if (avatarUrl.startsWith("/")) {
            return avatarUrl;
        }

        return "/" + avatarUrl;
    }

    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
            ? null
            : trimmed;
    }
}
