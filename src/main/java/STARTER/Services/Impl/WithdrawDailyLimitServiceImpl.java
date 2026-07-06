package STARTER.Services.Impl;

import STARTER.CustomException.DailyWithdrawLimitExceededException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.CustomException.WalletNotFoundException;
import STARTER.DTOs.WithdrawDailyLimitViewDTO;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class WithdrawDailyLimitServiceImpl implements WithdrawDailyLimitService {

    private final UserRepository userRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final BigDecimal minLimit;
    private final BigDecimal maxLimit;
    private final BigDecimal defaultLimit;
    private final int stepSize;
    private final ZoneId dayZoneId;

    public WithdrawDailyLimitServiceImpl(
            UserRepository userRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            @Value("${app.withdraw.daily-limit.min:50}") BigDecimal minLimit,
            @Value("${app.withdraw.daily-limit.max:500}") BigDecimal maxLimit,
            @Value("${app.withdraw.daily-limit.default:500}") BigDecimal defaultLimit,
            @Value("${app.withdraw.daily-limit.step:50}") int stepSize,
            @Value("${app.withdraw.day-timezone:Europe/Sofia}") String dayTimezone
    ) {
        this.userRepository = userRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
        this.defaultLimit = defaultLimit;
        this.stepSize = stepSize;
        this.dayZoneId = ZoneId.of(dayTimezone);
    }

    @Override
    public WithdrawDailyLimitViewDTO getViewForUsername(String username) {
        User user = findUser(username);

        if (user.getRole() == UserRole.ADMIN) {
            return WithdrawDailyLimitViewDTO.builder()
                    .applies(false)
                    .build();
        }

        return buildView(user);
    }

    @Override
    public void assertWithinDailyLimit(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        WithdrawDailyLimitViewDTO view = buildView(user);
        BigDecimal remaining = view.getRemainingToday();

        if (amount.compareTo(remaining) > 0) {
            throw new DailyWithdrawLimitExceededException(remaining);
        }
    }

    @Override
    public BigDecimal normalizeUserDailyLimit(BigDecimal requestedLimit) {
        if (requestedLimit == null) {
            return defaultLimit;
        }

        int stepped = requestedLimit
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        int min = minLimit.intValue();
        int max = maxLimit.intValue();
        stepped = Math.clamp(stepped, min, max);
        stepped = Math.round(stepped / (float) stepSize) * stepSize;
        stepped = Math.clamp(stepped, min, max);

        return BigDecimal.valueOf(stepped).setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public BigDecimal defaultDailyLimit() {
        return defaultLimit;
    }

    private WithdrawDailyLimitViewDTO buildView(User user) {
        Wallet wallet = walletRepository.findByUser_Id(user.getId()).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        UserProfileDetails profile = profileDetailsRepository.findByUser_Id(user.getId()).orElseThrow(() ->
                new UserNotFoundException("Profile not found"));

        BigDecimal dailyLimit = resolveDailyLimit(profile);
        BigDecimal withdrawnToday = sumWithdrawnToday(wallet.getId());
        BigDecimal remainingToday = dailyLimit.subtract(withdrawnToday).max(BigDecimal.ZERO);

        return WithdrawDailyLimitViewDTO.builder()
                .applies(true)
                .dailyLimit(dailyLimit)
                .withdrawnToday(withdrawnToday)
                .remainingToday(remainingToday)
                .build();
    }

    private BigDecimal resolveDailyLimit(UserProfileDetails profile) {

        if (profile.getDailyWithdrawLimit() == null) {
            return defaultLimit;
        }

        return normalizeUserDailyLimit(profile.getDailyWithdrawLimit());
    }

    private BigDecimal sumWithdrawnToday(UUID walletId) {

        LocalDate today = LocalDate.now(dayZoneId);
        LocalDateTime startOfDay = today.atStartOfDay(dayZoneId).toLocalDateTime();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(dayZoneId).toLocalDateTime();

        return transactionRepository
                .sumCompletedWithdrawalsBetween(walletId, startOfDay, endOfDay)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));
    }
}
