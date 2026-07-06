package STARTER.Services.Interface;

import STARTER.DTOs.ProfileEditRequest;
import STARTER.DTOs.UserProfileDetailsViewDTO;
import STARTER.DTOs.WalletSettingsRequest;
import STARTER.DTOs.DailyLimitEditRequest;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionType;
import STARTER.Models.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.math.BigDecimal;

// Advanced - user profile details service
public interface UserProfileDetailsService {

    void createDefaultForUser(User user);
    void ensureProfileExistsForAllUsers();

    UserProfileDetailsViewDTO getProfileView(String username);
    ProfileEditRequest buildEditRequest(String username);

    void updateProfile(String username, ProfileEditRequest request, MultipartFile avatarFile);
    AccountStatus getAccountStatus(String username);
    void updateAccountStatus(String adminUsername, UUID userId, AccountStatus newStatus);

    // Advanced — wallet settings (/wallet/settings)
    WalletSettingsRequest buildWalletSettingsRequest(String username);
    void updateWalletSettings(String username, WalletSettingsRequest request);

    DailyLimitEditRequest buildDailyLimitEditRequest(String username);
    void updateDailyLimit(String username, BigDecimal dailyWithdrawLimit);

    boolean isBalanceHidden(String username);
    boolean isTransactionEmailEnabled(String username, TransactionType type);
}
