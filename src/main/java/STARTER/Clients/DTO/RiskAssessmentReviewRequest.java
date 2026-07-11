package STARTER.Clients.DTO;

import STARTER.Enums.AssessmentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentReviewRequest {

    private AssessmentStatus status;
    private String reviewedBy;
}
