package STARTER.Services.Impl;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Clients.DTO.RiskAssessmentReviewRequest;
import STARTER.Clients.RiskAssessmentClient;
import STARTER.CustomException.RiskReviewServiceException;
import STARTER.DTOs.AdminRiskAssessmentViewDTO;
import STARTER.Enums.AssessmentStatus;
import STARTER.Enums.RiskDecision;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Repositories.TransactionRepository;
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
    private final TransactionRepository transactionRepository;
    private final boolean enabled;

    public AdminRiskReviewServiceImpl(
            RiskAssessmentClient riskAssessmentClient,
            PendingTransferProcessingService pendingTransferProcessingService,
            TransactionRepository transactionRepository,
            @Value("${app.risk-service.enabled:true}") boolean enabled) {

        this.riskAssessmentClient = riskAssessmentClient;
        this.pendingTransferProcessingService = pendingTransferProcessingService;
        this.transactionRepository = transactionRepository;
        this.enabled = enabled;
    }

    @Override
    public List<AdminRiskAssessmentViewDTO> listRiskReviews() {

        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        try {
            return riskAssessmentClient.listManualReviews().stream().map(this::toView).toList();

        } catch (Exception ex) {

            logger.warn("Failed to load risk reviews", ex);
            throw new RiskReviewServiceException("Cannot load risk reviews. Make sure the microservice is running on port 8081.");
        }
    }

    @Override
    public long countPendingReviews() {

        if (!enabled) {
            return countLocalPendingRiskHeldTransfers();
        }

        try {

            return riskAssessmentClient.listManualReviews()
                .stream()
                .filter(assessment -> assessment.getStatus() == AssessmentStatus.PENDING)
                .filter(assessment -> assessment.getDecision() == null || assessment.getDecision() == RiskDecision.REVIEW)
                .count();

        } catch (Exception ex) {

            logger.warn("Could not count pending risk reviews from microservice, using local transfers: {}", ex.getMessage());
            return countLocalPendingRiskHeldTransfers();
        }
    }

    private long countLocalPendingRiskHeldTransfers() {

        return transactionRepository.countByTypeAndStatus(
                TransactionType.TRANSFER,
                TransactionStatus.PENDING_RISK_REVIEW
        );
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
    public int deleteAllRiskReviews() {

        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        List<RiskAssessmentClientResponse> reviews = loadManualReviews();

        for (RiskAssessmentClientResponse assessment : reviews) {

            if (assessment.getStatus() == AssessmentStatus.PENDING) {
                applyWalletAction(assessment.getTransactionRef(), false);
            }
        }

        try {

            riskAssessmentClient.deleteManualReviews();
            logger.info("Deleted {} manual risk review(s)", reviews.size());

            return reviews.size();

        } catch (Exception ex) {

            logger.warn("Failed to delete risk reviews", ex);
            throw new RiskReviewServiceException("Could not delete risk reviews.");
        }
    }

    private List<RiskAssessmentClientResponse> loadManualReviews() {

        try {
            return riskAssessmentClient.listManualReviews();

        } catch (Exception ex) {
            logger.warn("Failed to load risk reviews for deletion", ex);
            throw new RiskReviewServiceException("Cannot load risk reviews. Make sure the microservice is running on port 8081.");
        }
    }

    private void reviewWithWalletAction(
            UUID assessmentId, String reviewedBy, AssessmentStatus status, boolean approveTransfer) {

        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

        RiskAssessmentClientResponse assessment = loadAssessment(assessmentId);

        if (assessment.getStatus() != AssessmentStatus.PENDING) {
            throw new RiskReviewServiceException("Only pending risk reviews can be approved or rejected.");
        }

        boolean walletUpdated = applyWalletAction(assessment.getTransactionRef(), approveTransfer);
        patchAssessment(assessmentId, reviewedBy, status);

        if (!walletUpdated && assessment.getTransactionRef() != null) {
            logger.warn("Risk assessment {} updated but linked transfer {} was not found in wallet.", assessmentId, assessment.getTransactionRef());
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
            throw new RiskReviewServiceException("Could not update the risk assessment. It may have already been reviewed.");
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
                .status(response.getStatus() != null ? response.getStatus().name() : "-")
                .createdAt(response.getCreatedAt() != null
                        ? DateTimeDisplay.format(response.getCreatedAt())
                        : "-")
                .reviewedBy(response.getReviewedBy())
                .reviewedAt(response.getReviewedAt() != null
                        ? DateTimeDisplay.format(response.getReviewedAt())
                        : null)
                .reasons(response.getReasons())
                .build();
    }
}
