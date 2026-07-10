package STARTER.Services.Impl;

import STARTER.Clients.Dto.RiskAssessmentClientResponse;
import STARTER.Clients.Dto.RiskAssessmentReviewRequest;
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
    private final boolean enabled;

    public AdminRiskReviewServiceImpl(
            RiskAssessmentClient riskAssessmentClient,
            @Value("${app.risk-service.enabled:true}") boolean enabled
    ) {
        this.riskAssessmentClient = riskAssessmentClient;
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
        review(assessmentId, reviewedBy, AssessmentStatus.APPROVED);
    }

    @Override
    public void reject(UUID assessmentId, String reviewedBy) {
        review(assessmentId, reviewedBy, AssessmentStatus.REJECTED);
    }

    private void review(UUID assessmentId, String reviewedBy, AssessmentStatus status) {
        if (!enabled) {
            throw new RiskReviewServiceException("Risk assessment service is disabled.");
        }

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
