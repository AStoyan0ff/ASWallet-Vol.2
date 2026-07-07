package STARTER.GlobalExceptionHandler;

import STARTER.CustomException.*;
import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRegistrationConflict_redirectsToRegisterWithMessage() {
        String result = handler.handleRegistrationConflict(
                new UserAlreadyExistsException("Username already exists"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/register");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Username already exists");
    }

    @Test
    void handlePasswordMismatch_onChangePassword_redirectsWithEmptyForm() {
        when(request.getRequestURI()).thenReturn("/wallet/change-password");

        String result = handler.handlePasswordMismatch(
                new PasswordMismatchException("Passwords do not match"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/wallet/change-password");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Passwords do not match");
        verify(redirectAttributes).addFlashAttribute(eq("changePasswordRequest"), org.mockito.ArgumentMatchers.any(ChangePasswordRequest.class));
    }

    @Test
    void handlePasswordMismatch_onResetPassword_redirectsWithTokenPreserved() {
        when(request.getRequestURI()).thenReturn("/reset-password");
        when(request.getParameter("token")).thenReturn("abc-token");

        String result = handler.handlePasswordMismatch(
                new PasswordMismatchException("Passwords do not match"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/reset-password?token=abc-token");

        ArgumentCaptor<ResetPasswordRequest> captor = ArgumentCaptor.forClass(ResetPasswordRequest.class);
        verify(redirectAttributes).addFlashAttribute(eq("resetPasswordRequest"), captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("abc-token");
    }

    @Test
    void handleInvalidOrExpiredToken_redirectsToForgotPassword() {
        String result = handler.handleInvalidOrExpiredToken(
                new InvalidOrExpiredTokenException("Token expired"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/forgot-password");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Token expired");
    }

    @Test
    void handleInsufficientBalance_onWithdraw_redirectsToWithdraw() {
        when(request.getRequestURI()).thenReturn("/transactions/withdraw");

        String result = handler.handleInsufficientBalance(
                new InsufficientBalanceException("Insufficient balance"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/withdraw");
    }

    @Test
    void handleInsufficientBalance_onTransfer_redirectsToTransfer() {
        when(request.getRequestURI()).thenReturn("/transactions/transfer/confirm");

        String result = handler.handleInsufficientBalance(
                new InsufficientBalanceException("Insufficient balance"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/transfer");
    }

    @Test
    void handleDailyWithdrawLimitExceeded_redirectsToWithdraw() {

        String result = handler.handleDailyWithdrawLimitExceeded(
                new DailyWithdrawLimitExceededException(new BigDecimal("25.00")),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/withdraw");
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), org.mockito.ArgumentMatchers.contains("Remaining today"));
    }

    @Test
    void handleTransferErrors_redirectsToTransferForm() {

        String result = handler.handleTransferErrors(
                new ReceiverNotFoundException("Receiver not found"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/transfer");
    }

    @Test
    void handlePendingTransferErrors_redirectsToHistory() {

        String result = handler.handlePendingTransferErrors(
                new CannotCancelTransferException("Only pending transfers can be cancelled."),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/history");
    }

    @Test
    void handleUserNotFound_onAdminMessages_redirectsToAdminSend() {
        when(request.getRequestURI()).thenReturn("/admin/messages/send");

        String result = handler.handleUserNotFound(
                new UserNotFoundException("User not found"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/admin/messages/send");
    }

    @Test
    void handleUserNotFound_onTransfer_redirectsToTransfer() {
        when(request.getRequestURI()).thenReturn("/transactions/transfer");

        String result = handler.handleUserNotFound(
                new UserNotFoundException("User not found"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/transfer");
    }

    @Test
    void handleWalletNotFound_redirectsToLogin() {

        String result = handler.handleWalletNotFound(
                new WalletNotFoundException("Wallet not found"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/login");
    }

    @Test
    void handleAccessDenied_redirectsWithGenericMessage() {
        when(request.getRequestURI()).thenReturn("/admin");

        String result = handler.handleAccessDenied(
                new AccessDeniedException("Denied"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/admin");
        verify(redirectAttributes).addFlashAttribute(
                eq("errorMessage"),
                org.mockito.ArgumentMatchers.contains("You do not have access")
        );
    }

    @Test
    void handleAdminDeleteErrors_onManagePage_redirectsBackToManage() {
        when(request.getRequestURI()).thenReturn("/admin/users/7f2c9f1a-1111-2222-3333-4444-555555555555/status");

        String result = handler.handleAdminDeleteErrors(
                new CannotChangeSelfAccountStatusException("Cannot change own status"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/admin/users/7f2c9f1a-1111-2222-3333-4444-555555555555/manage");
    }

    @Test
    void handleAdminDeleteErrors_cannotChangeAdminRoleWithoutPermissions_redirectsToWallet() {

        String result = handler.handleAdminDeleteErrors(
                new CannotChangeAdminRoleException("You do not have admin permissions to change roles."),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/wallet");
    }

    @Test
    void handleAdminDeleteErrors_cannotMessageAdmin_redirectsToAdminSend() {

        String result = handler.handleAdminDeleteErrors(
                new CannotMessageAdminException("Cannot message admin"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/admin/messages/send");
    }

    @Test
    void handleMailboxMessageNotFound_redirectsToWalletMessages() {

        String result = handler.handleMailboxMessageNotFound(
                new MailboxMessageNotFoundException("Message not found"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/wallet/messages");
    }

    @Test
    void handleInvalidCardDetails_redirectsToBankDetails() {

        String result = handler.handleInvalidCardDetails(
                new InvalidCardDetailsException("Invalid card"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/wallet/bank-details");
    }

    @Test
    void handleInvalidAvatarFile_redirectsToProfileEdit() {

        String result = handler.handleInvalidAvatarFile(
                new InvalidAvatarFileException("Invalid avatar"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/profile/edit");
    }

    @Test
    void handleMaxUploadSize_redirectsToProfileEditWithFriendlyMessage() {

        String result = handler.handleMaxUploadSize(
                mock(MaxUploadSizeExceededException.class),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/profile/edit");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Avatar file is too large. Maximum size is 2 MB.");
    }

    @Test
    void handleReceiverBankCardNotFound_redirectsToTransfer() {

        String result = handler.handleReceiverBankCardNotFound(
                new ReceiverBankCardNotFoundException("Receiver has no card"),
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/transfer");
    }

    @Test
    void handleUnexpected_onDeposit_redirectsToDepositWithGenericMessage() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/transactions/deposit");

        String result = handler.handleUnexpected(
                new RuntimeException("boom"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/transactions/deposit");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Something went wrong. Please try again.");
    }

    @Test
    void handleUnexpected_onUnknownPath_redirectsToHome() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/unknown");

        String result = handler.handleUnexpected(
                new RuntimeException("boom"),
                request,
                redirectAttributes
        );

        assertThat(result).isEqualTo("redirect:/");
    }
}
