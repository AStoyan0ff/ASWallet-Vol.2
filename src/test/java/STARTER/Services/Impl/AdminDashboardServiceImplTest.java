package STARTER.Services.Impl;

import STARTER.DTOs.AdminDashboardSummaryDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Services.Interface.AdminMailboxService;
import STARTER.Services.Interface.AdminRiskReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserProfileDetailsRepository profileDetailsRepository;
    @Mock private AdminRiskReviewService adminRiskReviewService;
    @Mock private AdminMailboxService adminMailboxService;

    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(
                transactionRepository,
                profileDetailsRepository,
                adminRiskReviewService,
                adminMailboxService,
                "Europe/Sofia"
        );
    }

    @Test
    void getSummary_aggregatesKpisAndChartSeries() {

        when(adminRiskReviewService.countPendingReviews()).thenReturn(3L);
        when(adminMailboxService.countUnreadForAdminInbox()).thenReturn(2L);
        when(profileDetailsRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(8L);

        lenient().when(transactionRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TransactionType.TRANSFER), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(2L);

        lenient().when(transactionRepository.countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TransactionType.TRANSFER),
                any(TransactionStatus.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        lenient().when(transactionRepository.countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TransactionType.TRANSFER),
                eq(TransactionStatus.COMPLETED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(4L);

        lenient().when(transactionRepository.countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TransactionType.TRANSFER),
                eq(TransactionStatus.PENDING),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(1L);

        AdminDashboardSummaryDTO summary = service.getSummary();

        assertThat(summary.getPendingRiskReviews()).isEqualTo(3L);
        assertThat(summary.getUnreadInbox()).isEqualTo(2L);
        assertThat(summary.getActiveUsers()).isEqualTo(8L);
        assertThat(summary.getTransfersToday()).isEqualTo(2L);
        assertThat(summary.getTodayStatusLabels()).containsExactly(
                "Pending", "Risk hold", "Completed", "Failed", "Cancelled"
        );

        assertThat(summary.getTodayStatusCounts()).containsExactly(1L, 0L, 4L, 0L, 0L);
        assertThat(summary.getLast7DaysLabels()).hasSize(7);
        assertThat(summary.getLast7DaysCounts()).containsOnly(2L);
    }
}
