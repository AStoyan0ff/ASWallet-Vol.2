package STARTER.Controllers;

import STARTER.DTOs.TransactionViewDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Services.Interface.TransactionService;
import STARTER.Services.Interface.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TransactionHistoryController.class)
class TransactionHistoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserService userService;

    @Test
    void history_firstPage_returnsPaginatedView() throws Exception {
        UUID userId = UUID.randomUUID();
        UserViewDTO user = new UserViewDTO();
        user.setId(userId);
        user.setUsername("Plamen");

        TransactionViewDTO transaction = new TransactionViewDTO();
        transaction.setId(UUID.randomUUID());
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setCreatedAt("2026-07-14 01:00");

        Page<TransactionViewDTO> page = new PageImpl<>(
                List.of(transaction),
                PageRequest.of(0, TransactionHistoryController.PAGE_SIZE),
                6
        );

        when(userService.findByUsername("Plamen")).thenReturn(user);
        when(transactionService.getUserTransactionsPage(userId, 0, TransactionHistoryController.PAGE_SIZE))
                .thenReturn(page);
        when(transactionService.hasPendingTransfers(userId)).thenReturn(false);

        mockMvc.perform(get("/transactions/history").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction-history"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("totalItems", 6L))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", true));
    }

    @Test
    void history_secondPage_passesPageParameterToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UserViewDTO user = new UserViewDTO();
        user.setId(userId);
        user.setUsername("Plamen");

        Page<TransactionViewDTO> page = new PageImpl<>(
                List.of(),
                PageRequest.of(1, TransactionHistoryController.PAGE_SIZE),
                6
        );

        when(userService.findByUsername("Plamen")).thenReturn(user);
        when(transactionService.getUserTransactionsPage(userId, 1, TransactionHistoryController.PAGE_SIZE))
                .thenReturn(page);
        when(transactionService.hasPendingTransfers(userId)).thenReturn(false);

        mockMvc.perform(get("/transactions/history").param("page", "1").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("hasPrevious", true));

        verify(transactionService).getUserTransactionsPage(userId, 1, TransactionHistoryController.PAGE_SIZE);
    }

    @Test
    void cancelPendingTransfer_redirectsBackToSamePage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UserViewDTO user = new UserViewDTO();
        user.setId(userId);
        user.setUsername("Plamen");

        when(userService.findByUsername("Plamen")).thenReturn(user);

        mockMvc.perform(post("/transactions/{id}/cancel", txId)
                        .param("page", "2")
                        .with(user("Plamen"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions/history?page=2"));

        verify(transactionService).cancelPendingTransfer(eq(txId), eq(userId));
    }
}
