package STARTER.Services.Interface;

import STARTER.DTOs.LoginActivityViewDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface LoginActivityService {

    void recordSuccessfulLogin(String username, HttpServletRequest request);
    List<LoginActivityViewDTO> getLastLogins(int limit);
    int clearAll();
}
