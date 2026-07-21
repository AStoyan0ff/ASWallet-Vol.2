package STARTER.Services.Impl;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Clients.DTO.RiskAssessmentReviewRequest;
import STARTER.Clients.RiskAssessmentClient;
import STARTER.CustomException.RiskReviewServiceException;
import STARTER.Enums.AssessmentStatus;
import STARTER.Enums.RiskDecision;
import STARTER.Repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRiskReviewServiceImplTest {

    @Mock private RiskAssessmentClient riskAssessmentClient;
    @Mock private PendingTransferProcessingService pendingTransferProcessingService;
    @Mock private TransactionRepository transactionRepository;

    private AdminRiskReviewServiceImpl adminRiskReviewService;

    private UUID assessmentId;
    private UUID transactionRef;

    @BeforeEach
    void setUp() {
        adminRiskReviewService = new AdminRiskReviewServiceImpl(
                riskAssessmentClient,
                pendingTransferProcessingService,
                transactionRepository,
                true
        );
        assessmentId = UUID.randomUUID();
        transactionRef = UUID.randomUUID();
    }

    @Test
    void reject_refundsWalletBeforePatchingMicroservice() {
        when(riskAssessmentClient.getAssessment(assessmentId)).thenReturn(assessmentWithTransactionRef());

        adminRiskReviewService.reject(assessmentId, "admin");

        verify(pendingTransferProcessingService).rejectRiskHeldTransfer(transactionRef);

        ArgumentCaptor<RiskAssessmentReviewRequest> requestCaptor =
                ArgumentCaptor.forClass(RiskAssessmentReviewRequest.class);

        verify(riskAssessmentClient).reviewAssessment(eq(assessmentId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(AssessmentStatus.REJECTED);
    }

    @Test
    void approve_completesWalletTransferBeforePatchingMicroservice() {
        when(riskAssessmentClient.getAssessment(assessmentId)).thenReturn(assessmentWithTransactionRef());

        adminRiskReviewService.approve(assessmentId, "admin");

        verify(pendingTransferProcessingService).approveRiskHeldTransfer(transactionRef);

        ArgumentCaptor<RiskAssessmentReviewRequest> requestCaptor =
                ArgumentCaptor.forClass(RiskAssessmentReviewRequest.class);

        verify(riskAssessmentClient).reviewAssessment(eq(assessmentId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(AssessmentStatus.APPROVED);
    }

    @Test
    void reject_withoutTransactionRef_onlyPatchesMicroservice() {
        RiskAssessmentClientResponse assessment = assessmentWithTransactionRef();
        assessment.setTransactionRef(null);
        when(riskAssessmentClient.getAssessment(assessmentId)).thenReturn(assessment);

        adminRiskReviewService.reject(assessmentId, "admin");

        verify(pendingTransferProcessingService, never()).rejectRiskHeldTransfer(any());
        verify(riskAssessmentClient).reviewAssessment(eq(assessmentId), any());
    }

    @Test
    void reject_whenAssessmentMissing_throwsWithoutWalletAction() {
        when(riskAssessmentClient.getAssessment(assessmentId)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> adminRiskReviewService.reject(assessmentId, "admin"))
                .isInstanceOf(RiskReviewServiceException.class);

        verify(pendingTransferProcessingService, never()).rejectRiskHeldTransfer(any());
        verify(riskAssessmentClient, never()).reviewAssessment(any(), any());
    }

    @Test
    void reject_orphanedReview_stillPatchesMicroservice() {

        when(riskAssessmentClient.getAssessment(assessmentId)).thenReturn(assessmentWithTransactionRef());
        when(pendingTransferProcessingService.rejectRiskHeldTransfer(transactionRef)).thenReturn(false);

        adminRiskReviewService.reject(assessmentId, "admin");

        verify(riskAssessmentClient).reviewAssessment(eq(assessmentId), any());
    }

    @Test
    void deleteAllRiskReviews_refundsPendingTransfersAndDeletesFromMicroservice() {
        when(riskAssessmentClient.listManualReviews())
                .thenReturn(java.util.List.of(assessmentWithTransactionRef()));

        int deleted = adminRiskReviewService.deleteAllRiskReviews();

        assertThat(deleted).isEqualTo(1);
        verify(pendingTransferProcessingService).rejectRiskHeldTransfer(transactionRef);
        verify(riskAssessmentClient).deleteManualReviews();
    }

    @Test
    void countPendingReviews_usesManualReviewsEndpoint() {
        RiskAssessmentClientResponse pending = assessmentWithTransactionRef();
        RiskAssessmentClientResponse approved = assessmentWithTransactionRef();
        approved.setId(UUID.randomUUID());
        approved.setStatus(AssessmentStatus.APPROVED);

        when(riskAssessmentClient.listManualReviews()).thenReturn(java.util.List.of(pending, approved));

        assertThat(adminRiskReviewService.countPendingReviews()).isEqualTo(1L);
        verify(riskAssessmentClient).listManualReviews();
        verify(riskAssessmentClient, never()).listAssessments(any(), any());
    }

    @Test
    void countPendingReviews_fallsBackToLocalTransfersWhenMicroserviceFails() {
        when(riskAssessmentClient.listManualReviews()).thenThrow(new RuntimeException("down"));
        when(transactionRepository.countByTypeAndStatus(
                STARTER.Enums.TransactionType.TRANSFER,
                STARTER.Enums.TransactionStatus.PENDING_RISK_REVIEW
        )).thenReturn(2L);

        assertThat(adminRiskReviewService.countPendingReviews()).isEqualTo(2L);
    }

    private RiskAssessmentClientResponse assessmentWithTransactionRef() {

        RiskAssessmentClientResponse response = new RiskAssessmentClientResponse();
        response.setId(assessmentId);
        response.setTransactionRef(transactionRef);
        response.setSenderUsername("Plamen");
        response.setReceiverUsername("Ivan");
        response.setAmount(new BigDecimal("200.00"));
        response.setRiskScore(50);
        response.setDecision(RiskDecision.REVIEW);
        response.setStatus(AssessmentStatus.PENDING);
        return response;
    }
}
