package STARTER.Models;

import STARTER.Enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "users")
public class User extends BaseClass {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @OneToOne(mappedBy = "user")
    private Wallet wallet;

    // Advanced — user profile details relationship
    @OneToOne(mappedBy = "user")
    private UserProfileDetails profileDetails;

    @Override
    protected void onPrePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.role == null) {
            this.role = UserRole.USER;
        }
    }
}
