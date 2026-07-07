package STARTER.Services.Impl;

import STARTER.CustomException.InvalidOrExpiredTokenException;
import STARTER.CustomException.PasswordMismatchException;
import STARTER.DTOs.ResetPasswordRequest;
import STARTER.Models.PasswordResetToken;
import STARTER.Models.User;
import STARTER.Repositories.PasswordResetTokenRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;
    private static final String RAW_TOKEN = "11111111-2222-3333-4444-555555555555";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("Plamen")
                .email("plamen@example.com")
                .password("encoded-old")
                .build();
        user.setId(UUID.randomUUID());

        ReflectionTestUtils.setField(passwordResetService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(passwordResetService, "expiryHours", 1);
    }

    // --- REQUEST RESET ---

    @Test
    void requestPasswordReset_unknownEmail_doesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("unknown@example.com");

        verify(tokenRepository, never()).deleteByUser(any());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void requestPasswordReset_knownEmail_deletesOldToken_savesNewToken_sendsEmail() {
        when(userRepository.findByEmail("plamen@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset("plamen@example.com");

        verify(tokenRepository).deleteByUser(user);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(
                eq("plamen@example.com"),
                eq("Plamen"),
                linkCaptor.capture()
        );
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:8080/reset-password?token=");
    }

    // --- RESET PASSWORD ---

    @Test
    void resetPassword_passwordMismatch_throwsBeforeLookup() {

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("NewPass1!")
                .confirmPassword("OtherPass1!")
                .build();

        assertThrows(PasswordMismatchException.class,
                () -> passwordResetService.resetPassword(RAW_TOKEN, request));

        verify(tokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void resetPassword_unknownToken_throwsInvalidOrExpiredTokenException() {
        ResetPasswordRequest request = validResetRequest("NewPass1!");

        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> passwordResetService.resetPassword(RAW_TOKEN, request));
    }

    @Test
    void resetPassword_usedToken_throwsInvalidOrExpiredTokenException() {
        ResetPasswordRequest request = validResetRequest("NewPass1!");
        PasswordResetToken resetToken = validResetToken(true, LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> passwordResetService.resetPassword(RAW_TOKEN, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidOrExpiredTokenException() {

        ResetPasswordRequest request = validResetRequest("NewPass1!");
        PasswordResetToken resetToken = validResetToken(false, LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> passwordResetService.resetPassword(RAW_TOKEN, request));
    }

    @Test
    void resetPassword_sameAsCurrentPassword_throwsPasswordMismatchException() {
        ResetPasswordRequest request = validResetRequest("NewPass1!");
        PasswordResetToken resetToken = validResetToken(false, LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("NewPass1!", "encoded-old")).thenReturn(true);

        assertThrows(PasswordMismatchException.class,
                () -> passwordResetService.resetPassword(RAW_TOKEN, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_success_updatesPasswordAndMarksTokenUsed() {

        ResetPasswordRequest request = validResetRequest("NewPass1!");
        PasswordResetToken resetToken = validResetToken(false, LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("NewPass1!", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded-new");

        passwordResetService.resetPassword(RAW_TOKEN, request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(resetToken.isUsed()).isTrue();

        verify(userRepository).save(user);
        verify(tokenRepository).save(resetToken);
    }

    // --- TOKEN VALIDATION ---

    @Test
    void isTokenValid_activeToken_returnsTrue() {

        PasswordResetToken resetToken = validResetToken(false, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(resetToken));

        assertThat(passwordResetService.isTokenValid(RAW_TOKEN)).isTrue();
    }

    @Test
    void isTokenValid_usedOrExpired_returnsFalse() {

        PasswordResetToken usedToken = validResetToken(true, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.of(usedToken));

        assertThat(passwordResetService.isTokenValid(RAW_TOKEN)).isFalse();
    }

    @Test
    void isTokenValid_unknownToken_returnsFalse() {
        when(tokenRepository.findByTokenHash(hashToken(RAW_TOKEN))).thenReturn(Optional.empty());

        assertThat(passwordResetService.isTokenValid(RAW_TOKEN)).isFalse();
    }

    // --- CLEANUP ---

    @Test
    void cleanupExpiredAndUsedTokens_returnsRepositoryDeleteCount() {
        when(tokenRepository.deleteByExpiresAtBeforeOrUsedTrue(any(LocalDateTime.class))).thenReturn(3L);

        int removed = passwordResetService.cleanupExpiredAndUsedTokens();

        assertThat(removed).isEqualTo(3);
    }

    @Test
    void cleanupExpiredAndUsedTokens_noneRemoved_returnsZero() {
        when(tokenRepository.deleteByExpiresAtBeforeOrUsedTrue(any(LocalDateTime.class))).thenReturn(0L);

        assertThat(passwordResetService.cleanupExpiredAndUsedTokens()).isZero();
    }

    private ResetPasswordRequest validResetRequest(String newPassword) {
        return ResetPasswordRequest.builder()
                .newPassword(newPassword)
                .confirmPassword(newPassword)
                .build();
    }

    private PasswordResetToken validResetToken(boolean used, LocalDateTime expiresAt) {

        return PasswordResetToken.builder()
                .tokenHash(hashToken(RAW_TOKEN))
                .user(user)
                .used(used)
                .expiresAt(expiresAt)
                .build();
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
