package STARTER.Services.Interface;

import STARTER.DTOs.ResetPasswordRequest;

public interface PasswordResetService {

    void requestPasswordReset(String email);

    void resetPassword(String rawToken, ResetPasswordRequest request);

    boolean isTokenValid(String rawToken);

    // Advanced — remove expired and used password reset tokens (scheduled job)
    int cleanupExpiredAndUsedTokens();
}
