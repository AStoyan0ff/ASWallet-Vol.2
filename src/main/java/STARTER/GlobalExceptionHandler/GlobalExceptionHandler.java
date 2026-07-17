package STARTER.GlobalExceptionHandler;

import STARTER.CustomException.*;
import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            UserAlreadyExistsException.class,
            EmailAlreadyExistsException.class})

    public String handleRegistrationConflict(RuntimeException ex, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public String handlePasswordMismatch(PasswordMismatchException ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String uri = request.getRequestURI();

        if (uri.contains("/wallet/change-password")) {
            redirectAttributes.addFlashAttribute("changePasswordRequest", new ChangePasswordRequest());
        }

        if (uri.contains("/reset-password")) {
            String token = request.getParameter("token");

            if (token != null && !token.isBlank()) {

                redirectAttributes.addFlashAttribute(
                        "resetPasswordRequest",
                        ResetPasswordRequest.builder().token(token).build()
                );
            }
        }

        return "redirect:" + resolvePasswordMismatchRedirect(request);
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public String handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex,
                                              RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/forgot-password";
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public String handleInsufficientBalance(InsufficientBalanceException ex,
                                              HttpServletRequest request,
                                              RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + resolveTransactionRedirect(request);
    }

    @ExceptionHandler(DailyWithdrawLimitExceededException.class)
    public String handleDailyWithdrawLimitExceeded(DailyWithdrawLimitExceededException ex,
                                                   RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/transactions/withdraw";
    }

    @ExceptionHandler({
            NotTransferMoneyYourselfException.class,
            SenderNotFoundException.class,
            ReceiverNotFoundException.class,
            TransferBlockedByRiskException.class})

    public String handleTransferErrors(RuntimeException ex, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/transactions/transfer";
    }

    @ExceptionHandler({
            CannotCancelTransferException.class,
            PendingTransferNotFoundException.class})

    public String handlePendingTransferErrors(RuntimeException ex, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/transactions/history";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFound(UserNotFoundException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String uri = request.getRequestURI();

        if (uri != null && uri.contains("/admin/messages")) {
            return "redirect:/admin/messages/send";
        }

        return "redirect:" + resolveUserNotFoundRedirect(request);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public String handleWalletNotFound(WalletNotFoundException ex, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/login";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "You do not have access to this page. If you were recently promoted to admin, refresh the page or log in again."
        );

        return "redirect:" + resolveFallbackRedirect(request);
    }

    @ExceptionHandler
        ({

            CannotDeleteSelfException.class,
            CannotDeleteAdminException.class,
            CannotChangeSelfAccountStatusException.class,
            CannotChangeAdminAccountStatusException.class,
            CannotChangeSelfRoleException.class,
            CannotChangeAdminRoleException.class,
            CannotMessageAdminException.class
        })

    public String handleAdminDeleteErrors(RuntimeException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + resolveAdminRedirect(request, ex);
    }

    @ExceptionHandler(MailboxMessageNotFoundException.class)
    public String handleMailboxMessageNotFound(MailboxMessageNotFoundException ex,
                                               RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/wallet/messages";
    }

    private String resolveAdminRedirect(HttpServletRequest request, RuntimeException ex) {

        if (ex instanceof CannotChangeAdminRoleException roleEx) {
            String message = roleEx.getMessage();

            if (message != null && message.startsWith("You do not have admin permissions")) {
                return "/wallet";
            }

            return "/admin";
        }

        if (ex instanceof CannotMessageAdminException) {
            return "/admin/messages/send";
        }

        String uri = request.getRequestURI();

        if (uri != null && uri.contains("/admin/users/")) {
            String[] segments = uri.split("/");

            for (int idx = 0; idx < segments.length - 1; idx++) {

                if ("users".equals(segments[idx])) {
                    String userId = segments[idx + 1];

                    if (userId != null && !userId.isBlank()) {
                        return "/admin/users/" + userId + "/manage";
                    }
                }
            }
        }

        return "/admin";
    }

    @ExceptionHandler(InvalidCardDetailsException.class)
    public String handleInvalidCardDetails(InvalidCardDetailsException ex,
                                           RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/wallet/bank-details";
    }

    @ExceptionHandler(InvalidAvatarFileException.class)
    public String handleInvalidAvatarFile(InvalidAvatarFileException ex,
                                          RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/profile/edit";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                      RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", "Avatar file is too large. Maximum size is 2 MB.");
        return "redirect:/profile/edit";
    }

    @ExceptionHandler(ReceiverBankCardNotFoundException.class)
    public String handleReceiverBankCardNotFound(ReceiverBankCardNotFoundException ex,
                                                 RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/transactions/transfer";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleUnexpected(RuntimeException ex,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {

        logger.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong. Please try again.");

        return "redirect:" + resolveFallbackRedirect(request);
    }

    private String resolvePasswordMismatchRedirect(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.contains("/register")) {
            return "/register";
        }

        if (uri.contains("/wallet/delete-account")) {
            return "/wallet/delete-account";
        }

        if (uri.contains("/wallet/change-password")) {
            return "/wallet/change-password";
        }

        if (uri.contains("/reset-password")) {
            return "/reset-password?token=" + request.getParameter("token");
        }

        return "/wallet";
    }

    private String resolveTransactionRedirect(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.contains("/transactions/withdraw")) {
            return "/transactions/withdraw";
        }

        if (uri.contains("/transactions/transfer/confirm") || uri.contains("/transactions/transfer")) {
            return "/transactions/transfer";
        }

        return "/wallet";
    }

    private String resolveUserNotFoundRedirect(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri != null && uri.startsWith("/admin")) {

            if (uri.contains("/messages")) {
                return "/admin/messages/send";
            }

            return "/admin";
        }

        if (uri.contains("/wallet/change-password")) {
            return "/wallet/change-password";
        }

        if (uri.contains("/transactions/transfer")) {
            return "/transactions/transfer";
        }

        return "/wallet";
    }

    private String resolveFallbackRedirect(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.contains("/register")) {
            return "/register";
        }

        if (uri.contains("/login")) {
            return "/login";
        }

        if (uri.contains("/forgot-password")) {
            return "/forgot-password";
        }

        if (uri.contains("/reset-password")) {
            return "/forgot-password";
        }

        if (uri.contains("/transactions/transfer")) {
            return "/transactions/transfer";
        }

        if (uri.contains("/transactions/deposit")) {
            return "/transactions/deposit";
        }

        if (uri.contains("/transactions/withdraw")) {
            return "/transactions/withdraw";
        }

        if (uri.contains("/wallet/change-password")) {
            return "/wallet/change-password";
        }

        if (uri.contains("/wallet/bank-details")) {
            return "/wallet/bank-details";
        }

        if (uri.contains("/wallet/delete-account")) {
            return "/wallet/delete-account";
        }

        if (uri.contains("/wallet/settings")) {
            return "/wallet/settings";
        }

        if (uri.contains("/wallet/export")) {
            return "/wallet/export";
        }

        if (uri.contains("/profile/edit")) {
            return "/profile/edit";
        }

        if (uri.contains("/profile")) {
            return "/profile";
        }

        if (uri.startsWith("/admin")) {
            return "/admin";
        }

        if (uri.startsWith("/wallet") || uri.startsWith("/transactions")) {
            return "/wallet";
        }

        return "/";
    }
}
