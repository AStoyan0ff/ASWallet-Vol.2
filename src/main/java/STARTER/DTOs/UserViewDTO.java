package STARTER.DTOs;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor

public class UserViewDTO {

    private UUID id;
    private String username;
    private String email;
    private String registeredAt;
}
