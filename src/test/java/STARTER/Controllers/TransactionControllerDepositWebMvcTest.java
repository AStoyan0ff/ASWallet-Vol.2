package STARTER.Controllers;

import STARTER.DTOs.BankCardViewDTO;
import STARTER.DTOs.DepositMoneyDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.DTOs.WalletViewDTO;
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
class TransactionControllerDepositWebMvcTest {

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
    }

    @Test
    void getDeposit_returnsDepositViewWithWallet() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(get("/transactions/deposit").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("deposit"))
                .andExpect(model().attributeExists("wallet"))
                .andExpect(model().attributeExists("savedBankCard"));
    }

    @Test
    void postDeposit_success_redirectsToWalletAndCallsService() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(post("/transactions/deposit")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("amount", "25.00")
                        .param("cardCvc", "123")
                        .param("spendingCategory", SpendingCategory.SHOPPING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wallet"))
                .andExpect(flash().attribute("successMessage", "Deposit completed successfully."));

        verify(transactionService).deposit(eq(userView.getId()), any(DepositMoneyDTO.class));
    }

    @Test
    void postDeposit_withoutBankCard_redirectsBackWithError() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.empty());

        mockMvc.perform(post("/transactions/deposit")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("amount", "25.00")
                        .param("cardCvc", "123")
                        .param("spendingCategory", SpendingCategory.SHOPPING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/deposit"))
                .andExpect(flash().attribute("errorMessage", "Please add bank details before depositing."));

        verify(transactionService, never()).deposit(any(), any());
    }

    @Test
    void postDeposit_validationError_redirectsBackWithoutCallingService() throws Exception {
        when(bankCardService.getBankCardByUsername("Plamen")).thenReturn(Optional.of(bankCardView));

        mockMvc.perform(post("/transactions/deposit")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("cardCvc", "12")
                        .param("spendingCategory", SpendingCategory.SHOPPING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/deposit"));

        verify(transactionService, never()).deposit(any(), any());
    }
}
