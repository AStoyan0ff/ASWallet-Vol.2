package STARTER.Services.Interface;

import STARTER.DTOs.AdminRiskAssessmentViewDTO;

import java.util.List;
import java.util.UUID;

public interface AdminRiskReviewService {

    List<AdminRiskAssessmentViewDTO> listPendingReviews();

    long countPendingReviews();

    void approve(UUID assessmentId, String reviewedBy);

    void reject(UUID assessmentId, String reviewedBy);
}
