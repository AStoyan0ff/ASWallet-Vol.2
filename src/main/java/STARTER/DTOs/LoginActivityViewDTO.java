package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// Advanced — login activity row for admin view
public class LoginActivityViewDTO {

    private String username;
    private String ipAddress;
    private String loggedInAt;
}
