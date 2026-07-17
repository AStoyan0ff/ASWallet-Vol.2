package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminDashboardSummaryDTO {

    private final long pendingRiskReviews;
    private final long transfersToday;
    private final long unreadInbox;
    private final long activeUsers;

    private final List<String> todayStatusLabels;
    private final List<Long> todayStatusCounts;

    private final List<String> last7DaysLabels;
    private final List<Long> last7DaysCounts;
}
