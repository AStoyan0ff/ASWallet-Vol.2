package STARTER.Services.Impl;

import STARTER.DTOs.AdminDashboardSummaryDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Services.Interface.AdminDashboardService;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Services.Interface.AdminRiskReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private static final List<TransactionStatus> TODAY_CHART_STATUSES = List.of(
            TransactionStatus.PENDING,
            TransactionStatus.PENDING_RISK_REVIEW,
            TransactionStatus.COMPLETED,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED);

    private final TransactionRepository transactionRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final AdminRiskReviewService adminRiskReviewService;
    private final AdminMailboxService adminMailboxService;
    private final ZoneId dayZoneId;

    public AdminDashboardServiceImpl(
            TransactionRepository transactionRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            AdminRiskReviewService adminRiskReviewService,
            AdminMailboxService adminMailboxService,
            @Value("${app.withdraw.day-timezone:Europe/Sofia}") String dayTimezone) {

        this.transactionRepository = transactionRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.adminRiskReviewService = adminRiskReviewService;
        this.adminMailboxService = adminMailboxService;
        this.dayZoneId = ZoneId.of(dayTimezone);
    }

    @Override
    public AdminDashboardSummaryDTO getSummary() {

        LocalDate today = LocalDate.now(dayZoneId);
        LocalDateTime startOfToday = today.atStartOfDay(dayZoneId).toLocalDateTime();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay(dayZoneId).toLocalDateTime();

        long transfersToday = transactionRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                TransactionType.TRANSFER,
                startOfToday,
                startOfTomorrow
        );

        List<String> todayStatusLabels = new ArrayList<>();
        List<Long> todayStatusCounts = new ArrayList<>();

        for (TransactionStatus status : TODAY_CHART_STATUSES) {

            long count = transactionRepository.countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    TransactionType.TRANSFER,
                    status,
                    startOfToday,
                    startOfTomorrow
            );

            todayStatusLabels.add(toShortLabel(status));
            todayStatusCounts.add(count);
        }

        List<String> last7DaysLabels = new ArrayList<>();
        List<Long> last7DaysCounts = new ArrayList<>();

        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {

            LocalDate day = today.minusDays(daysAgo);
            LocalDateTime dayStart = day.atStartOfDay(dayZoneId).toLocalDateTime();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay(dayZoneId).toLocalDateTime();

            long count = transactionRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    TransactionType.TRANSFER,
                    dayStart,
                    dayEnd
            );

            last7DaysLabels.add(day.format(DAY_LABEL));
            last7DaysCounts.add(count);
        }

        return AdminDashboardSummaryDTO.builder()
                .pendingRiskReviews(adminRiskReviewService.countPendingReviews())
                .transfersToday(transfersToday)
                .unreadInbox(adminMailboxService.countUnreadForAdminInbox())
                .activeUsers(profileDetailsRepository.countByAccountStatus(AccountStatus.ACTIVE))
                .todayStatusLabels(todayStatusLabels)
                .todayStatusCounts(todayStatusCounts)
                .last7DaysLabels(last7DaysLabels)
                .last7DaysCounts(last7DaysCounts)
                .build();
    }

    private static String toShortLabel(TransactionStatus status) {
        return switch (status) {

            case PENDING -> "Pending";
            case PENDING_RISK_REVIEW -> "Risk hold";
            case COMPLETED -> "Completed";
            case FAILED -> "Failed";
            case CANCELLED -> "Cancelled";
        };
    }
}
