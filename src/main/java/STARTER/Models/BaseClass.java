package STARTER.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor

public abstract class BaseClass {

    @Id
    private UUID id;

    @PrePersist
    private void basePrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        onPrePersist();
    }

    protected void onPrePersist() {
        // Override in entities that need extra initialization before insert.
    }
}
