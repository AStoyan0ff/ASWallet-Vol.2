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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
