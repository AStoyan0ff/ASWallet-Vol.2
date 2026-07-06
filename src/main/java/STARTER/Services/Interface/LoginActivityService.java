package STARTER.Services.Interface;

import STARTER.DTOs.LoginActivityViewDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

// Advanced — login activity tracking
public interface LoginActivityService {

    void recordSuccessfulLogin(String username, HttpServletRequest request);
    List<LoginActivityViewDTO> getLastLogins(int limit);
}
