package STARTER.Models;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
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

    protected void onPrePersist()
    {}
}
