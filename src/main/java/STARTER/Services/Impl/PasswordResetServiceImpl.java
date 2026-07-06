package STARTER.Services.Impl;

import STARTER.CustomException.InvalidOrExpiredTokenException;
import STARTER.CustomException.PasswordMismatchException;
import STARTER.DTOs.ResetPasswordRequest;
import STARTER.Models.PasswordResetToken;
import STARTER.Models.User;
import STARTER.Repositories.PasswordResetTokenRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.EmailService;
import STARTER.Services.Interface.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-hours:1}")
    private int expiryHours;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteByUser(user);

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .tokenHash(hashToken(rawToken))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getUsername(),
                    baseUrl + "/reset-password?token=" + rawToken);

            logger.info("Password reset requested: username={}", user.getUsername());
        });
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("New password and confirm password do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidOrExpiredTokenException("This reset link is invalid or has expired."));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOrExpiredTokenException("This reset link is invalid or has expired.");
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new PasswordMismatchException("New password must be different from your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        logger.info("Password reset completed: username={}", user.getUsername());
    }

    @Override
    public boolean isTokenValid(String rawToken) {

        return tokenRepository.findByTokenHash(hashToken(rawToken))
                .filter(token -> !token.isUsed() && token.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Override
    @Transactional
    public int cleanupExpiredAndUsedTokens() {
        long removed = tokenRepository.deleteByExpiresAtBeforeOrUsedTrue(LocalDateTime.now());

        if (removed > 0) {
            logger.info("Removed {} expired or used password reset token(s)", removed);
        }

        return Math.toIntExact(removed);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
