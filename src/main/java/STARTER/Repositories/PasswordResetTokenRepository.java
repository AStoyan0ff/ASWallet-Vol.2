package STARTER.Repositories;

import STARTER.Models.PasswordResetToken;
import STARTER.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);

    // Advanced — scheduled cleanup of expired and used reset tokens
    @Modifying
    long deleteByExpiresAtBeforeOrUsedTrue(LocalDateTime expiresAt);
}
