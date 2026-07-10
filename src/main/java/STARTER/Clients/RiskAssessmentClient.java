package STARTER.Clients;

import STARTER.Clients.Dto.RiskAssessmentCreateRequest;
import STARTER.Clients.Dto.RiskAssessmentClientResponse;
import STARTER.Clients.Dto.RiskAssessmentReviewRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "aswallet-risk-service", url = "${app.risk-service.base-url}")
public interface RiskAssessmentClient {

    @PostMapping("/api/risk-assessments")
    RiskAssessmentClientResponse createAssessment(@RequestBody RiskAssessmentCreateRequest request);

    @GetMapping("/api/risk-assessments")
    List<RiskAssessmentClientResponse> listAssessments(@RequestParam("status") String status);

    @PatchMapping("/api/risk-assessments/{id}/review")
    RiskAssessmentClientResponse reviewAssessment(
            @PathVariable("id") UUID id,
            @RequestBody RiskAssessmentReviewRequest request
    );
}
