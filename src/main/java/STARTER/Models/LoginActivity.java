package STARTER.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "login_activities")
public class LoginActivity extends BaseClass {

    @Column(nullable = false)
    private String username;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;

    @Override
    protected void onPrePersist() {

        if (loggedInAt == null) {
            loggedInAt = LocalDateTime.now();
        }
    }
}
