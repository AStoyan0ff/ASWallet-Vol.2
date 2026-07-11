package STARTER.Services.Impl;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.Clients.DTO.RiskAssessmentCreateRequest;
import STARTER.Clients.RiskAssessmentClient;
import STARTER.CustomException.TransferBlockedByRiskException;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.DTOs.WithdrawDailyLimitViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Enums.RiskDecision;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Services.Interface.TransferRiskAssessmentService;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferRiskAssessmentServiceImpl implements TransferRiskAssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(TransferRiskAssessmentServiceImpl.class);

    private final RiskAssessmentClient riskAssessmentClient;
    private final WithdrawDailyLimitService withdrawDailyLimitService;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final TransactionRepository transactionRepository;
    private final boolean enabled;
    private final boolean failOpen;
    private final ZoneId dayZoneId;

    public TransferRiskAssessmentServiceImpl(
            RiskAssessmentClient riskAssessmentClient,
            WithdrawDailyLimitService withdrawDailyLimitService,
            UserProfileDetailsRepository profileDetailsRepository,
            TransactionRepository transactionRepository,
            @Value("${app.risk-service.enabled:true}") boolean enabled,
            @Value("${app.risk-service.fail-open:true}") boolean failOpen,
            @Value("${app.withdraw.day-timezone:Europe/Sofia}") String dayTimezone
    ) {
        this.riskAssessmentClient = riskAssessmentClient;
        this.withdrawDailyLimitService = withdrawDailyLimitService;
        this.profileDetailsRepository = profileDetailsRepository;
        this.transactionRepository = transactionRepository;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.dayZoneId = ZoneId.of(dayTimezone);
    }

    @Override
    public RiskAssessmentClientResponse assessTransfer(
            UUID transactionRef,
            User senderUser,
            Wallet senderWallet,
            User receiverUser,
            Wallet receiverWallet,
            TransferMoneyDTO transferMoneyDTO,
            boolean receiverHasBankCard
    ) {
        if (!enabled) {
            return allowedResponse();
        }

        RiskAssessmentCreateRequest request = buildRequest(
                transactionRef,
                senderUser,
                senderWallet,
                receiverUser,
                receiverWallet,
                transferMoneyDTO,
                receiverHasBankCard
        );

        try {
            RiskAssessmentClientResponse response = riskAssessmentClient.createAssessment(request);
            assertTransferAllowed(response);
            return response;

        } catch (TransferBlockedByRiskException ex) {
            throw ex;

        } catch (Exception ex) {

            if (failOpen) {
                logger.warn("Risk service unavailable, allowing transfer: {}", ex.getMessage());
                return allowedResponse();
            }

            throw new TransferBlockedByRiskException(
                    "Risk assessment service is unavailable. Transfer was not submitted."
            );
        }
    }

    private void assertTransferAllowed(RiskAssessmentClientResponse response) {

        if (response.getDecision() == RiskDecision.BLOCK) {
            throw new TransferBlockedByRiskException(buildBlockedMessage(response));
        }

        if (response.getDecision() == RiskDecision.REVIEW) {

            logger.info(
                    "Transfer flagged for manual review: assessmentId={}, transactionRef={}, score={}, reasons={}",
                    response.getId(),
                    response.getTransactionRef(),
                    response.getRiskScore(),
                    response.getReasons()
            );
        }
    }

    private RiskAssessmentCreateRequest buildRequest(
            UUID transactionRef,
            User senderUser,
            Wallet senderWallet,
            User receiverUser,
            Wallet receiverWallet,
            TransferMoneyDTO transferMoneyDTO,
            boolean receiverHasBankCard
    ) {
        WithdrawDailyLimitViewDTO limitView = withdrawDailyLimitService.getViewForUsername(senderUser.getUsername());
        BigDecimal dailyLimit = limitView.isApplies()
                ? limitView.getDailyLimit()
                : withdrawDailyLimitService.defaultDailyLimit();
        BigDecimal withdrawnToday = limitView.isApplies()
                ? limitView.getWithdrawnToday()
                : BigDecimal.ZERO;

        AccountStatus accountStatus = profileDetailsRepository.findByUser_Id(senderUser.getId())
                .map(UserProfileDetails::getAccountStatus)
                .orElse(AccountStatus.ACTIVE);

        LocalDate today = LocalDate.now(dayZoneId);
        LocalDateTime startOfDay = today.atStartOfDay(dayZoneId).toLocalDateTime();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(dayZoneId).toLocalDateTime();

        long transfersTodayCount = transactionRepository.countTransfersBetween(
                senderWallet.getId(),
                startOfDay,
                endOfDay
        );

        boolean newReceiver = !transactionRepository.existsTransferBetweenSenderAndReceiver(
                senderWallet.getId(),
                receiverWallet.getId()
        );

        RiskAssessmentCreateRequest request = new RiskAssessmentCreateRequest();
        request.setTransactionRef(transactionRef);
        request.setSenderUsername(senderUser.getUsername());
        request.setReceiverUsername(receiverUser.getUsername());
        request.setAmount(transferMoneyDTO.getAmount());
        request.setSenderBalance(senderWallet.getBalance());
        request.setWithdrawnToday(withdrawnToday);
        request.setDailyLimit(dailyLimit);
        request.setTransfersTodayCount((int) transfersTodayCount);
        request.setReceiverHasBankCard(receiverHasBankCard);
        request.setNewReceiver(newReceiver);
        request.setAccountStatus(accountStatus.name());
        request.setHourOfDay(LocalDateTime.now(dayZoneId).getHour());
        return request;
    }

    private String buildBlockedMessage(RiskAssessmentClientResponse response) {
        List<String> reasons = response.getReasons();

        if (reasons == null || reasons.isEmpty()) {
            return "Transfer blocked by risk assessment (score " + response.getRiskScore() + ").";
        }

        return "Transfer blocked by risk assessment: "
                + reasons.stream().collect(Collectors.joining("; "));
    }

    private RiskAssessmentClientResponse allowedResponse() {

        RiskAssessmentClientResponse response = new RiskAssessmentClientResponse();
        response.setDecision(RiskDecision.ALLOW);
        response.setRiskScore(0);
        return response;
    }
}
