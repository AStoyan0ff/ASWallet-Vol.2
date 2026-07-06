package STARTER.Configuration;

import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.WithdrawDailyLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Configuration
public class WithdrawDailyLimitBackfillConfig {

    private static final Logger logger = LoggerFactory.getLogger(WithdrawDailyLimitBackfillConfig.class);

    @Bean
    @Transactional
    public ApplicationRunner withdrawDailyLimitBackfillRunner(
            UserRepository userRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            WithdrawDailyLimitService withdrawDailyLimitService
    ) {
        return args -> {
            int updated = 0;

            for (User user : userRepository.findAll()) {

                if (user.getRole() == UserRole.ADMIN) {
                    continue;
                }

                UserProfileDetails profile = profileDetailsRepository.findByUser_Id(user.getId()).orElse(null);

                if (profile == null) {
                    continue;
                }

                if (profile.getDailyWithdrawLimit() == null) {
                    profile.setDailyWithdrawLimit(withdrawDailyLimitService.defaultDailyLimit());

                    profileDetailsRepository.save(profile);
                    updated++;
                    continue;
                }

                BigDecimal normalized = withdrawDailyLimitService.normalizeUserDailyLimit(profile.getDailyWithdrawLimit());

                if (profile.getDailyWithdrawLimit().compareTo(normalized) != 0) {

                    profile.setDailyWithdrawLimit(normalized);
                    profileDetailsRepository.save(profile);
                    updated++;
                }
            }

            if (updated > 0) {
                logger.info("Withdraw daily limit backfill: {} user profiles updated.", updated);
            }
        };
    }
}
