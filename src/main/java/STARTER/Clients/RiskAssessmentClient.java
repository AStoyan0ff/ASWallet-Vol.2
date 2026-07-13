package STARTER.Clients;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Clients.DTO.RiskAssessmentCreateRequest;
import STARTER.Clients.DTO.RiskAssessmentReviewRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "aswallet-risk-service", url = "${app.risk-service.base-url}")
public interface RiskAssessmentClient {

    @PostMapping("/api/risk-assessments")
    RiskAssessmentClientResponse createAssessment(@RequestBody RiskAssessmentCreateRequest request);

    @GetMapping("/api/risk-assessments")
    List<RiskAssessmentClientResponse> listAssessments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "decision", required = false) String decision
    );

    @GetMapping("/api/risk-assessments/manual-reviews")
    List<RiskAssessmentClientResponse> listManualReviews();

    @GetMapping("/api/risk-assessments/{id}")
    RiskAssessmentClientResponse getAssessment(@PathVariable() UUID id); // "id"

    @PatchMapping("/api/risk-assessments/{id}/review")
    RiskAssessmentClientResponse reviewAssessment(
            @PathVariable() UUID id, // "id"
            @RequestBody RiskAssessmentReviewRequest request
    );

    @DeleteMapping("/api/risk-assessments")
    void deleteAssessments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "decision", required = false) String decision
    );

    @DeleteMapping("/api/risk-assessments/manual-reviews")
    void deleteManualReviews();
}
