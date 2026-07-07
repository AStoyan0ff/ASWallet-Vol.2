package STARTER.Controllers;

import STARTER.DTOs.UserDTO;
import STARTER.Services.Interface.LoginActivityService;
import STARTER.Services.Interface.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private LoginActivityService loginActivityService;

    // --- REGISTER ---

    @Test
    void getRegister_returnsRegisterViewWithUserDTO() throws Exception {
        mockMvc.perform(get("/register").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("userDTO"));
    }

    @Test
    void postRegister_success_redirectsToLoginAndCallsService() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "Plamen")
                        .param("email", "plamen@example.com")
                        .param("password", "Secret1!")
                        .param("confirmPassword", "Secret1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Account created successfully. Please log in."));

        verify(userService).register(any(UserDTO.class));
    }

    @Test
    void postRegister_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "ab")
                        .param("email", "invalid-email")
                        .param("password", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(userService, never()).register(any());
    }

    // --- LOGIN ---

    @Test
    void getLogin_returnsLoginViewWithLoginDTO() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginDTO"));
    }

    @Test
    void postLogin_success_redirectsToWalletAndRecordsActivity() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockMvc.perform(post("/login")
                        .param("username", "Plamen")
                        .param("password", "Secret1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"));

        verify(loginActivityService).recordSuccessfulLogin(eq("Plamen"), any());
    }

    @Test
    void postLogin_validationError_redirectsBackWithoutAuthenticating() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "ab")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(authenticationManager, never()).authenticate(any());
        verify(loginActivityService, never()).recordSuccessfulLogin(any(), any());
    }

    @Test
    void postLogin_badCredentials_redirectsWithErrorMessage() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/login")
                        .param("username", "Plamen")
                        .param("password", "WrongPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Invalid username or password. Please try again."
                ));

        verify(loginActivityService, never()).recordSuccessfulLogin(any(), any());
    }

    @Test
    void postLogin_inactiveAccount_redirectsWithInactiveMessage() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Disabled"));

        mockMvc.perform(post("/login")
                        .param("username", "Plamen")
                        .param("password", "Secret1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Your account is inactive. Please contact an administrator."
                ));

        verify(loginActivityService, never()).recordSuccessfulLogin(any(), any());
    }
}
