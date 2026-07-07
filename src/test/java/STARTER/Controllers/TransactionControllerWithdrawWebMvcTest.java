package STARTER.Controllers;

import STARTER.DTOs.BankCardViewDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.DTOs.WalletViewDTO;
import STARTER.DTOs.WithdrawDailyLimitViewDTO;
import STARTER.DTOs.WithdrawMoneyDTO;
import STARTER.Enums.SpendingCategory;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import STARTER.Services.Interface.WalletService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TransactionController.class)
class TransactionControllerWithdrawWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BankCardService bankCardService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private WithdrawDailyLimitService withdrawDailyLimitService;

    private UserViewDTO userView;
    private BankCardViewDTO bankCardView;
    private WithdrawDailyLimitViewDTO dailyLimitView;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

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

        dailyLimitView = WithdrawDailyLimitViewDTO.builder()
                .applies(true)
                .dailyLimit(new BigDecimal("500.00"))
                .withdrawnToday(new BigDecimal("50.00"))
                .remainingToday(new BigDecimal("450.00"))
                .build();

        when(userService.findByUsername("Plamen")).thenReturn(userView);
        when(withdrawDailyLimitService.getViewForUsername("Plamen")).thenReturn(dailyLimitView);
    }

    @Test
    void getWithdraw_returnsWithdrawViewWithDailyLimitAndCard() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(get("/transactions/withdraw").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("withdrawDailyLimit"))
                .andExpect(model().attributeExists("savedBankCard"));
    }

    @Test
    void postWithdraw_success_redirectsToWalletAndCallsService() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(post("/transactions/withdraw")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("amount", "40.00")
                        .param("spendingCategory", SpendingCategory.BILLS.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attribute("successMessage", "Withdrawal completed successfully."));

        verify(transactionService).withdraw(eq(userView.getId()), any(WithdrawMoneyDTO.class));
    }

    @Test
    void postWithdraw_withoutBankCard_redirectsBackWithError() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.empty());

        mockMvc.perform(post("/transactions/withdraw")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("amount", "40.00")
                        .param("spendingCategory", SpendingCategory.BILLS.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/withdraw"))
                .andExpect(flash().attribute("errorMessage", "Please add bank details before withdrawing."));

        verify(transactionService, never()).withdraw(any(), any());
    }

    @Test
    void postWithdraw_validationError_redirectsBackWithoutCallingService() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(post("/transactions/withdraw")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("spendingCategory", SpendingCategory.BILLS.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/withdraw"));

        verify(transactionService, never()).withdraw(any(), any());
    }
}
