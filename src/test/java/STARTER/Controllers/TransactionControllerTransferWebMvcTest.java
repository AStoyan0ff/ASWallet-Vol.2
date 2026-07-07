package STARTER.Controllers;

import STARTER.DTOs.TransferMoneyDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.DTOs.WalletViewDTO;
import STARTER.Enums.SpendingCategory;
import STARTER.Services.Interface.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTransferWebMvcTest {

    private static final String SESSION_PENDING_TRANSFER = "pendingTransferMoneyDTO";

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

        when(walletService.getWalletByUsername("Plamen")).thenReturn(walletView);
        when(userService.findByUsername("Plamen")).thenReturn(userView);
    }

    @Test
    void getTransfer_returnsTransferView() throws Exception {

        mockMvc.perform(get("/transactions/transfer").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer"));
    }

    @Test
    void postTransfer_success_redirectsToConfirmAndStoresPendingTransferInSession() throws Exception {

        MvcResult result = mockMvc.perform(post("/transactions/transfer")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("receiverUsername", "Georgi")
                        .param("amount", "30.00")
                        .param("spendingCategory", SpendingCategory.SHOPPING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/transfer/confirm"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        Object pending = session.getAttribute(SESSION_PENDING_TRANSFER);
        assertThat(pending).isInstanceOf(TransferMoneyDTO.class);

        TransferMoneyDTO dto = (TransferMoneyDTO) pending;
        assertThat(dto.getReceiverUsername()).isEqualTo("Georgi");
        assertThat(dto.getAmount()).isEqualByComparingTo("30.00");
        assertThat(dto.getSpendingCategory()).isEqualTo(SpendingCategory.SHOPPING);

        verify(transactionService, never()).transfer(any(), any());
    }

    @Test
    void postTransfer_validationError_redirectsBackWithoutCallingService() throws Exception {

        mockMvc.perform(post("/transactions/transfer")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("receiverUsername", "ab")
                        .param("amount", "30.00")
                        .param("spendingCategory", SpendingCategory.SHOPPING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/transfer"));

        verify(transactionService, never()).transfer(any(), any());
    }

    @Test
    void getTransferConfirm_withSession_returnsConfirmViewWithBalanceAfter() throws Exception {

        MockHttpSession session = sessionWithPendingTransfer("Georgi", "30.00");

        mockMvc.perform(get("/transactions/transfer/confirm")
                        .session(session)
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer-confirm"))
                .andExpect(model().attributeExists("transferMoneyDTO"))
                .andExpect(model().attributeExists("wallet"))
                .andExpect(model().attribute("balanceAfter", new BigDecimal("70.00")));
    }

    @Test
    void getTransferConfirm_withoutSession_redirectsToTransferForm() throws Exception {

        mockMvc.perform(get("/transactions/transfer/confirm").with(user("Plamen")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/transfer"));
    }

    @Test
    void postTransferConfirm_success_redirectsToHistoryAndCallsService() throws Exception {

        MockHttpSession session = sessionWithPendingTransfer("Georgi", "30.00");

        mockMvc.perform(post("/transactions/transfer/confirm")
                        .with(csrf())
                        .session(session)
                        .with(user("Plamen")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/history"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Transfer submitted. It will be processed shortly — check Transaction History for status."
                ));

        verify(transactionService).transfer(eq(userView.getId()), any(TransferMoneyDTO.class));
        assertThat(session.getAttribute(SESSION_PENDING_TRANSFER)).isNull();
    }

    @Test
    void postTransferConfirm_withoutSession_redirectsWithError() throws Exception {

        mockMvc.perform(post("/transactions/transfer/confirm")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/transfer"))
                .andExpect(flash().attribute("errorMessage", "Transfer session expired. Please try again."));

        verify(transactionService, never()).transfer(any(), any());
    }

    private MockHttpSession sessionWithPendingTransfer(String receiverUsername, String amount) {

        TransferMoneyDTO pending = new TransferMoneyDTO();
        pending.setReceiverUsername(receiverUsername);
        pending.setAmount(new BigDecimal(amount));
        pending.setSpendingCategory(SpendingCategory.SHOPPING);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_PENDING_TRANSFER, pending);
        return session;
    }
}
