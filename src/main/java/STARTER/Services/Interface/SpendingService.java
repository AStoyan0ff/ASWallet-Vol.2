package STARTER.Services.Interface;

import STARTER.DTOs.SpendingInsightsViewDTO;
import STARTER.Enums.SpendingPeriod;

public interface SpendingService {
    SpendingInsightsViewDTO getInsights(String username, SpendingPeriod period);
}
