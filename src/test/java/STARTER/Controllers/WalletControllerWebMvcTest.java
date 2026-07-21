package STARTER.Controllers;

import STARTER.DTOs.*;
import STARTER.Services.Interface.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BankCardService bankCardService;

    @MockitoBean
    private UserProfileDetailsService userProfileDetailsService;

    @MockitoBean
    private AdminMailboxService adminMailboxService;

    @MockitoBean
    private AdminRiskReviewService adminRiskReviewService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private TransactionService transactionService;

    private WalletViewDTO walletView;
    private UserViewDTO userView;
    private BankCardViewDTO bankCardView;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        walletView = new WalletViewDTO();
        walletView.setId(UUID.randomUUID());
        walletView.setUserId(userId);
        walletView.setUsername("Plamen");
        walletView.setBalance(new BigDecimal("100.00"));
        walletView.setCurrency("EUR");

        userView = new UserViewDTO();
        userView.setId(userId);
        userView.setUsername("Plamen");
        userView.setEmail("plamen@example.com");

        bankCardView = BankCardViewDTO.builder()
                .maskedCardNumber("****1111")
                .lastFourDigits("1111")
                .cardholderName("Plamen Test")
                .expiryMonth("12")
                .expiryYear("30")
                .build();

        when(walletService.getWalletByUsername("Plamen")).thenReturn(walletView);
        when(userService.findByUsername("Plamen")).thenReturn(userView);
        when(userProfileDetailsService.isBalanceHidden("Plamen")).thenReturn(false);
        when(adminMailboxService.countUnreadForUser("Plamen")).thenReturn(2L);
        when(transactionService.getUserTransactionsPage(any(UUID.class), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
    }

    // --- GET / WALLET ---

    @Test
    void getWallet_regularUser_returnsWalletViewWithUnreadCount() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(get("/wallet")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet"))
                .andExpect(model().attribute("wallet", walletView))
                .andExpect(model().attribute("balanceHidden", false))
                .andExpect(model().attribute("isAdmin", false))
                .andExpect(model().attribute("unreadMessageCount", 2L))
                .andExpect(model().attributeExists("savedBankCard"))
                .andExpect(model().attributeExists("recentTransactions"));
    }

    @Test
    void getWallet_adminUser_returnsAdminInboxCount() throws Exception {
        when(walletService.getWalletByUsername("admin")).thenReturn(walletView);
        when(userProfileDetailsService.isBalanceHidden("admin")).thenReturn(false);
        when(bankCardService.getBankCardByUsername("admin")).thenReturn(Optional.empty());
        when(adminMailboxService.countUnreadForAdminInbox()).thenReturn(4L);
        when(adminRiskReviewService.countPendingReviews()).thenReturn(3L);

        mockMvc.perform(get("/wallet")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet"))
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(model().attribute("adminInboxUnreadCount", 4L))
                .andExpect(model().attribute("pendingRiskReviewCount", 3L));
    }

    // --- BANK DETAILS ---

    @Test
    void getBankDetails_returnsBankDetailsView() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(get("/wallet/bank-details")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("bank-details"))
                .andExpect(model().attribute("user", userView))
                .andExpect(model().attributeExists("bankCardRequest"))
                .andExpect(model().attributeExists("savedBankCard"));
    }

    @Test
    void postBankDetails_successWithWelcomeBonus_redirectsWithMessage() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.empty());
        when(bankCardService.saveBankCard(eq("Plamen"), any())).thenReturn(true);

        mockMvc.perform(post("/wallet/bank-details")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("cardNumber", "4111111111111111")
                        .param("cardholderName", "Plamen Test")
                        .param("expiryMonth", "12")
                        .param("expiryYear", "30")
                        .param("cardCvc", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/bank-details"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Bank card saved. €50.00 welcome bonus added to your wallet."
                ));

        verify(bankCardService).saveBankCard(eq("Plamen"), any());
    }

    @Test
    void postBankDetails_successWithoutBonus_redirectsWithBasicMessage() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.empty());
        when(bankCardService.saveBankCard(eq("Plamen"), any())).thenReturn(false);

        mockMvc.perform(post("/wallet/bank-details")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("cardNumber", "4111111111111111")
                        .param("cardholderName", "Plamen Test")
                        .param("expiryMonth", "12")
                        .param("expiryYear", "30")
                        .param("cardCvc", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/bank-details"))
                .andExpect(flash().attribute("successMessage", "Bank card saved successfully."));
    }

    @Test
    void postBankDetails_whenCardAlreadyRegistered_redirectsWithError() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(post("/wallet/bank-details")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("cardNumber", "4111111111111111")
                        .param("cardholderName", "Plamen Test")
                        .param("expiryMonth", "12")
                        .param("expiryYear", "30")
                        .param("cardCvc", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/bank-details"))
                .andExpect(flash().attribute("errorMessage", "Bank card is already registered."));

        verify(bankCardService, never()).saveBankCard(any(), any());
    }

    @Test
    void postBankDetails_validationError_redirectsBackWithoutSaving() throws Exception {
        mockMvc.perform(post("/wallet/bank-details")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("cardNumber", "1234")
                        .param("cardholderName", "Plamen Test")
                        .param("expiryMonth", "12")
                        .param("expiryYear", "30")
                        .param("cardCvc", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/bank-details"));

        verify(bankCardService, never()).saveBankCard(any(), any());
    }

    // --- CHANGE PASSWORD ---

    @Test
    void getChangePassword_returnsChangePasswordView() throws Exception {
        mockMvc.perform(get("/wallet/change-password")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("change-password"))
                .andExpect(model().attributeExists("changePasswordRequest"));
    }

    @Test
    void postChangePassword_success_redirectsWithMessage() throws Exception {
        mockMvc.perform(post("/wallet/change-password")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("oldPassword", "OldPass1!")
                        .param("newPassword", "NewPass1!")
                        .param("confirmPassword", "NewPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/change-password"))
                .andExpect(flash().attribute("successMessage", "Password changed successfully"));

        verify(userService).changePassword(eq("Plamen"), any(ChangePasswordRequest.class));
    }

    @Test
    void postChangePassword_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/wallet/change-password")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("oldPassword", "short")
                        .param("newPassword", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/change-password"));

        verify(userService, never()).changePassword(any(), any());
    }

    // --- SETTINGS ---

    @Test
    void getSettings_returnsSettingsViewWithRequest() throws Exception {
        WalletSettingsRequest settingsRequest = new WalletSettingsRequest();
        settingsRequest.setBalanceHidden(true);
        settingsRequest.setEmailOnDeposit(false);

        when(userProfileDetailsService.buildWalletSettingsRequest("Plamen")).thenReturn(settingsRequest);

        mockMvc.perform(get("/wallet/settings")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("walletSettingsRequest", settingsRequest))
                .andExpect(model().attribute("isAdmin", false));
    }

    @Test
    void postSettings_success_redirectsWithFlashMessage() throws Exception {
        mockMvc.perform(post("/wallet/settings")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("balanceHidden", "true")
                        .param("emailOnDeposit", "true")
                        .param("emailOnWithdraw", "false")
                        .param("emailOnTransfer", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/settings"))
                .andExpect(flash().attribute("successMessage", "Settings saved successfully."));

        verify(userProfileDetailsService).updateWalletSettings(eq("Plamen"), any(WalletSettingsRequest.class));
    }

    @Test
    void postSettings_ajaxRequest_returnsJsonResponse() throws Exception {
        mockMvc.perform(post("/wallet/settings")
                        .with(csrf())
                        .with(user("Plamen"))
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("balanceHidden", "true")
                        .param("emailOnDeposit", "false")
                        .param("emailOnWithdraw", "true")
                        .param("emailOnTransfer", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Settings saved successfully."))
                .andExpect(jsonPath("$.balanceHidden").value(true))
                .andExpect(jsonPath("$.emailOnWithdraw").value(true));

        verify(userProfileDetailsService).updateWalletSettings(eq("Plamen"), any(WalletSettingsRequest.class));
    }

    // --- DELETE ACCOUNT ---

    @Test
    void getDeleteAccount_returnsDeleteAccountView() throws Exception {
        mockMvc.perform(get("/wallet/delete-account")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("delete-account"))
                .andExpect(model().attributeExists("deleteAccountRequest"));
    }

    @Test
    void postDeleteAccount_success_redirectsToAccountDeleted() throws Exception {
        mockMvc.perform(post("/wallet/delete-account")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("password", "Secret1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account-deleted"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Your account has been deleted successfully."
                ));

        verify(userService).deleteAccount(eq("Plamen"), any());
    }

    @Test
    void postDeleteAccount_validationError_redirectsBackWithoutDeleting() throws Exception {
        mockMvc.perform(post("/wallet/delete-account")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet/delete-account"));

        verify(userService, never()).deleteAccount(any(), any());
    }

    @Test
    void getPayments_returnsPaymentsView() throws Exception {
        when(paymentService.listPaymentsForUsername("Plamen")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/wallet/payments").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("payments"))
                .andExpect(model().attributeExists("payments"));

        verify(paymentService).listPaymentsForUsername("Plamen");
    }

    @Test
    void getAccountDeleted_returnsAccountDeletedView() throws Exception {
        mockMvc.perform(get("/account-deleted").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("account-deleted"));
    }
}
