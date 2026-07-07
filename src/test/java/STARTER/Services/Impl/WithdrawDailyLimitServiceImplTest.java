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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawDailyLimitServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileDetailsRepository profileDetailsRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;

    private WithdrawDailyLimitServiceImpl withdrawDailyLimitService;

    private User user;
    private User admin;
    private Wallet wallet;
    private UserProfileDetails profile;
    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        withdrawDailyLimitService = new WithdrawDailyLimitServiceImpl(
                userRepository,
                profileDetailsRepository,
                walletRepository,
                transactionRepository,
                new BigDecimal("50"),
                new BigDecimal("500"),
                new BigDecimal("500"),
                50,
                "Europe/Sofia"
        );

        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        user = User.builder()
                .username("Plamen")
                .email("plamen@example.com")
                .password("encoded")
                .role(UserRole.USER)
                .build();
        user.setId(userId);

        admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password("encoded")
                .role(UserRole.ADMIN)
                .build();
        admin.setId(UUID.randomUUID());

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("1000.00"));

        profile = UserProfileDetails.builder()
                .user(user)
                .dailyWithdrawLimit(new BigDecimal("500.00"))
                .build();
    }

    // --- DEFAULT DAILY LIMIT / NORMALIZE DAILY LIMIT ---

    @Test
    void defaultDailyLimit_returnsConfiguredDefault() {
        assertThat(withdrawDailyLimitService.defaultDailyLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void normalizeUserDailyLimit_null_returnsDefault() {
        assertThat(withdrawDailyLimitService.normalizeUserDailyLimit(null))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void normalizeUserDailyLimit_roundsToNearestStep() {
        assertThat(withdrawDailyLimitService.normalizeUserDailyLimit(new BigDecimal("175")))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void normalizeUserDailyLimit_belowMin_clampsToMin() {
        assertThat(withdrawDailyLimitService.normalizeUserDailyLimit(new BigDecimal("25")))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void normalizeUserDailyLimit_aboveMax_clampsToMax() {
        assertThat(withdrawDailyLimitService.normalizeUserDailyLimit(new BigDecimal("600")))
                .isEqualByComparingTo("500.00");
    }

    // --- GET VIEW FOR USERNAME ---

    @Test
    void getViewForUsername_admin_returnsAppliesFalse() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        WithdrawDailyLimitViewDTO view = withdrawDailyLimitService.getViewForUsername("admin");

        assertThat(view.isApplies()).isFalse();
        verify(walletRepository, never()).findByUser_Id(any());
    }

    @Test
    void getViewForUsername_regularUser_returnsRemainingToday() {
        stubUserLimitData(new BigDecimal("500.00"), new BigDecimal("150.00"));

        WithdrawDailyLimitViewDTO view = withdrawDailyLimitService.getViewForUsername("Plamen");

        assertThat(view.isApplies()).isTrue();
        assertThat(view.getDailyLimit()).isEqualByComparingTo("500.00");
        assertThat(view.getWithdrawnToday()).isEqualByComparingTo("150.00");
        assertThat(view.getRemainingToday()).isEqualByComparingTo("350.00");
    }

    @Test
    void getViewForUsername_usesDefaultWhenProfileLimitMissing() {
        profile.setDailyWithdrawLimit(null);
        stubUserLimitData(null, new BigDecimal("0.00"));

        WithdrawDailyLimitViewDTO view = withdrawDailyLimitService.getViewForUsername("Plamen");

        assertThat(view.getDailyLimit()).isEqualByComparingTo("500.00");
        assertThat(view.getRemainingToday()).isEqualByComparingTo("500.00");
    }

    @Test
    void getViewForUsername_userNotFound_throws() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> withdrawDailyLimitService.getViewForUsername("missing"));
    }

    @Test
    void getViewForUsername_walletNotFound_throws() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> withdrawDailyLimitService.getViewForUsername("Plamen"));
    }

    // --- ASSERT WITHIN DAILY LIMIT ---

    @Test
    void assertWithinDailyLimit_withinLimit_doesNotThrow() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        stubLimitDataForUser(new BigDecimal("500.00"), new BigDecimal("100.00"));

        assertDoesNotThrow(() ->
                withdrawDailyLimitService.assertWithinDailyLimit(userId, new BigDecimal("400.00")));
    }

    @Test
    void assertWithinDailyLimit_exceedsLimit_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        stubLimitDataForUser(new BigDecimal("500.00"), new BigDecimal("100.00"));

        DailyWithdrawLimitExceededException ex = assertThrows(
                DailyWithdrawLimitExceededException.class,
                () -> withdrawDailyLimitService.assertWithinDailyLimit(userId, new BigDecimal("401.00"))
        );

        assertThat(ex.getRemainingToday()).isEqualByComparingTo("400.00");
    }

    @Test
    void assertWithinDailyLimit_admin_skipsCheck() {
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                withdrawDailyLimitService.assertWithinDailyLimit(admin.getId(), new BigDecimal("9999.00")));

        verify(transactionRepository, never()).sumCompletedWithdrawalsBetween(any(), any(), any());
    }

    private void stubUserLimitData(BigDecimal profileLimit, BigDecimal withdrawnToday) {
        if (profileLimit != null) {
            profile.setDailyWithdrawLimit(profileLimit);
        }

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        stubLimitDataForUser(profileLimit, withdrawnToday);
    }

    private void stubLimitDataForUser(BigDecimal profileLimit, BigDecimal withdrawnToday) {
        if (profileLimit != null) {
            profile.setDailyWithdrawLimit(profileLimit);
        }

        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(profileDetailsRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(transactionRepository.sumCompletedWithdrawalsBetween(eq(walletId), any(), any()))
                .thenReturn(withdrawnToday);
    }
}
