package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

public class LoginActivityViewDTO {

    private String username;
    private String ipAddress;
    private String loggedInAt;
}
