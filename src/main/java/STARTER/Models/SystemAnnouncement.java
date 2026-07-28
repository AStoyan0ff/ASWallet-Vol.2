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

@Table(name = "system_announcements")
public class SystemAnnouncement extends BaseClass {

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String updatedByUsername;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
