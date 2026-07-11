package STARTER.Controllers;

import STARTER.DTOs.AdminRiskAssessmentViewDTO;
import STARTER.Services.Interface.AdminRiskReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(AdminRiskReviewController.class)
class AdminRiskReviewControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRiskReviewService adminRiskReviewService;

    @Test
    void listPendingReviews_returnsViewWithAssessments() throws Exception {
        UUID id = UUID.randomUUID();

        when(adminRiskReviewService.listPendingReviews()).thenReturn(List.of(
                AdminRiskAssessmentViewDTO.builder()
                        .id(id)
                        .senderUsername("Plamen")
                        .receiverUsername("Ivan")
                        .amount(new BigDecimal("200.00"))
                        .riskScore(50)
                        .riskLevel("MEDIUM")
                        .decision("REVIEW")
                        .createdAt("2026-07-10 16:00")
                        .reasons(List.of("Night transfer"))
                        .build()
        ));

        mockMvc.perform(get("/admin/risk-reviews").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-risk-reviews"))
                .andExpect(model().attributeExists("assessments"));
    }

    @Test
    void approve_redirectsWithSuccessMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/risk-reviews/{id}/approve", id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/risk-reviews"))
                .andExpect(flash().attribute("successMessage", "Risk assessment approved."));

        verify(adminRiskReviewService).approve(eq(id), eq("admin"));
    }

    @Test
    void reject_redirectsWithSuccessMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/risk-reviews/{id}/reject", id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/risk-reviews"))
                .andExpect(flash().attribute("successMessage", "Risk assessment rejected."));

        verify(adminRiskReviewService).reject(eq(id), eq("admin"));
    }

    @Test
    void clear_redirectsWithSuccessMessage() throws Exception {
        when(adminRiskReviewService.deleteAllPendingReviews()).thenReturn(2);

        mockMvc.perform(post("/admin/risk-reviews/clear")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/risk-reviews"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Deleted 2 pending risk review(s). Funds returned to senders."
                ));

        verify(adminRiskReviewService).deleteAllPendingReviews();
    }

    @Test
    void approve_serviceError_redirectsWithErrorMessage() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new RuntimeException("Already reviewed"))
                .when(adminRiskReviewService).approve(eq(id), eq("admin"));

        mockMvc.perform(post("/admin/risk-reviews/{id}/approve", id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/risk-reviews"))
                .andExpect(flash().attribute("errorMessage", "Already reviewed"));
    }
}
