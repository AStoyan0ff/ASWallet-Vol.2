package STARTER.Controllers;

import STARTER.DTOs.ResetPasswordRequest;
import STARTER.Services.Interface.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordResetControllerWebMvcTest {

    private static final String VALID_TOKEN = "11111111-2222-3333-4444-555555555555";
    private static final String RESET_EMAIL_SENT_MESSAGE =
            "If an account with that email exists, we have sent password reset instructions.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    // --- FORGOT PASSWORD ---

    @Test
    void getForgotPassword_returnsForgotPasswordViewWithForm() throws Exception {
        mockMvc.perform(get("/forgot-password").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("forgotPasswordRequest"));
    }

    @Test
    void postForgotPassword_success_redirectsWithMessageAndCallsService() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .param("email", "plamen@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("successMessage", RESET_EMAIL_SENT_MESSAGE));

        verify(passwordResetService).requestPasswordReset("plamen@example.com");
    }

    @Test
    void postForgotPassword_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .param("email", "invalid-email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        verify(passwordResetService, never()).requestPasswordReset(any());
    }

    // --- RESET PASSWORD ---

    @Test
    void getResetPassword_validToken_returnsResetPasswordView() throws Exception {
        when(passwordResetService.isTokenValid(VALID_TOKEN)).thenReturn(true);

        mockMvc.perform(get("/reset-password")
                        .param("token", VALID_TOKEN)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("resetPasswordRequest"));
    }

    @Test
    void getResetPassword_invalidToken_redirectsToForgotPasswordWithError() throws Exception {
        when(passwordResetService.isTokenValid("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/reset-password")
                        .param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "This reset link is invalid or has expired."
                ));
    }

    @Test
    void postResetPassword_success_redirectsToLoginAndCallsService() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", VALID_TOKEN)
                        .param("newPassword", "NewPass1!")
                        .param("confirmPassword", "NewPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Password reset successfully. Please log in with your new password."
                ));

        verify(passwordResetService).resetPassword(eq(VALID_TOKEN), any(ResetPasswordRequest.class));
    }

    @Test
    void postResetPassword_validationError_redirectsBackWithToken() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", VALID_TOKEN)
                        .param("newPassword", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?token=" + VALID_TOKEN));

        verify(passwordResetService, never()).resetPassword(any(), any());
    }
}
