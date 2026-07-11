package STARTER.Services.Impl;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Clients.DTO.RiskAssessmentReviewRequest;
import STARTER.Clients.RiskAssessmentClient;
import STARTER.CustomException.RiskReviewServiceException;
import STARTER.DTOs.AdminRiskAssessmentViewDTO;
import STARTER.Enums.AssessmentStatus;
import STARTER.Services.Interface.AdminRiskReviewService;
import STARTER.Utils.DateTimeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminRiskReviewServiceImpl implements AdminRiskReviewService {

    private static final Logger logger = LoggerFactory.getLogger(AdminRiskReviewServiceImpl.class);

    private final RiskAssessmentClient riskAssessmentClient;
    private final PendingTransferProcessingService pendingTransferProcessingService;
    private final boolean enabled;

    public AdminRiskReviewServiceImpl(
            RiskAssessmentClient riskAssessmentClient,
            PendingTransferProcessingService pendingTransferProcessingService,
            @Value("${app.risk-service.enabled:true}") boolean enabled
    ) {
        this.riskAssessmentClient = riskAssessmentClient;
        this.pendingTransferProcessingService = pendingTransferProcessingService;
        this.enabled = enabled;
    }

    @Override
    public List<AdminRiskAssessmentViewDTO> listPendingReviews() {
        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        try {
            return riskAssessmentClient.listAssessments(AssessmentStatus.PENDING.name())
                    .stream()
                    .map(this::toView)
                    .toList();
        } catch (Exception ex) {
            logger.warn("Failed to load pending risk reviews", ex);
            throw new RiskReviewServiceException(
                    "Cannot load pending risk reviews. Make sure the microservice is running on port 8081."
            );
        }
    }

    @Override
    public long countPendingReviews() {
        if (!enabled) {
            return 0;
        }

        try {
            return riskAssessmentClient.listAssessments(AssessmentStatus.PENDING.name()).size();

        } catch (Exception ex) {
            logger.debug("Could not count pending risk reviews: {}", ex.getMessage());
            return 0;
        }
    }

    @Override
    public void approve(UUID assessmentId, String reviewedBy) {
        reviewWithWalletAction(assessmentId, reviewedBy, AssessmentStatus.APPROVED, true);
    }

    @Override
    public void reject(UUID assessmentId, String reviewedBy) {
        reviewWithWalletAction(assessmentId, reviewedBy, AssessmentStatus.REJECTED, false);
    }

    @Override
    public int deleteAllPendingReviews() {

        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        List<RiskAssessmentClientResponse> pending = loadPendingAssessments();

        for (RiskAssessmentClientResponse assessment : pending) {
            applyWalletAction(assessment.getTransactionRef(), false);
        }

        try {
            riskAssessmentClient.deleteAssessments(AssessmentStatus.PENDING.name());
            logger.info("Deleted {} pending risk assessment(s)", pending.size());
            return pending.size();

        } catch (Exception ex) {
            logger.warn("Failed to delete pending risk reviews", ex);
            throw new RiskReviewServiceException("Could not delete pending risk reviews.");
        }
    }

    private List<RiskAssessmentClientResponse> loadPendingAssessments() {

        try {
            return riskAssessmentClient.listAssessments(AssessmentStatus.PENDING.name());

        } catch (Exception ex) {
            logger.warn("Failed to load pending risk reviews for deletion", ex);
            throw new RiskReviewServiceException(
                    "Cannot load pending risk reviews. Make sure the microservice is running on port 8081."
            );
        }
    }

    private void reviewWithWalletAction(
            UUID assessmentId,
            String reviewedBy,
            AssessmentStatus status,
            boolean approveTransfer
    ) {

        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        RiskAssessmentClientResponse assessment = loadAssessment(assessmentId);
        boolean walletUpdated = applyWalletAction(assessment.getTransactionRef(), approveTransfer);
        patchAssessment(assessmentId, reviewedBy, status);

        if (!walletUpdated && assessment.getTransactionRef() != null) {
            logger.warn(
                    "Risk assessment {} updated but linked transfer {} was not found in wallet.",
                    assessmentId,
                    assessment.getTransactionRef()
            );
        }
    }

    private RiskAssessmentClientResponse loadAssessment(UUID assessmentId) {

        try {
            return riskAssessmentClient.getAssessment(assessmentId);

        } catch (Exception ex) {
            logger.warn("Failed to load risk assessment {}", assessmentId, ex);
            throw new RiskReviewServiceException("Risk assessment not found.");
        }
    }

    private boolean applyWalletAction(UUID transactionRef, boolean approveTransfer) {

        if (transactionRef == null) {
            logger.warn("Risk assessment has no transactionRef; only microservice status will be updated.");
            return false;
        }

        if (approveTransfer) {
            return pendingTransferProcessingService.approveRiskHeldTransfer(transactionRef);
        }

        return pendingTransferProcessingService.rejectRiskHeldTransfer(transactionRef);
    }

    private void patchAssessment(UUID assessmentId, String reviewedBy, AssessmentStatus status) {

        RiskAssessmentReviewRequest request = new RiskAssessmentReviewRequest();
        request.setStatus(status);
        request.setReviewedBy(reviewedBy);

        try {
            riskAssessmentClient.reviewAssessment(assessmentId, request);
            logger.info("Risk assessment {} marked as {} by {}", assessmentId, status, reviewedBy);

        } catch (Exception ex) {
            logger.warn("Failed to review risk assessment {}", assessmentId, ex);
            throw new RiskReviewServiceException(
                    "Could not update the risk assessment. It may have already been reviewed."
            );
        }
    }

    private AdminRiskAssessmentViewDTO toView(RiskAssessmentClientResponse response) {

        return AdminRiskAssessmentViewDTO.builder()
                .id(response.getId())
                .transactionRef(response.getTransactionRef())
                .senderUsername(response.getSenderUsername())
                .receiverUsername(response.getReceiverUsername())
                .amount(response.getAmount())
                .riskScore(response.getRiskScore())
                .riskLevel(response.getRiskLevel() != null ? response.getRiskLevel().name() : "-")
                .decision(response.getDecision() != null ? response.getDecision().name() : "-")
                .createdAt(response.getCreatedAt() != null
                        ? DateTimeDisplay.format(response.getCreatedAt())
                        : "-")
                .reasons(response.getReasons())
                .build();
    }
}
