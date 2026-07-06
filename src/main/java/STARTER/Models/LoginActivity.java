package STARTER.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "login_activities")
// Advanced — login activity audit (last logins for admin)
public class LoginActivity extends BaseClass {

    @Column(nullable = false)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;

    @PrePersist
    public void prePersist() {
        if (loggedInAt == null) {
            loggedInAt = LocalDateTime.now();
        }
    }
}
