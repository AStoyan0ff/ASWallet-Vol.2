package STARTER.Services.Interface;

import STARTER.DTOs.WithdrawDailyLimitViewDTO;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawDailyLimitService {

    WithdrawDailyLimitViewDTO getViewForUsername(String username);
    void assertWithinDailyLimit(UUID userId, BigDecimal amount);

    BigDecimal normalizeUserDailyLimit(BigDecimal requestedLimit);
    BigDecimal defaultDailyLimit();
}
