package STARTER.Configuration;

import STARTER.Models.User;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class WalletBalanceBackfillConfig {

    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceBackfillConfig.class);

    @Bean
    @Transactional
    public ApplicationRunner missingWalletBackfillRunner(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletService walletService
    ) {
        return args -> {
            int created = 0;

            for (User user : userRepository.findAll()) {

                if (walletRepository.findByUser_Id(user.getId()).isEmpty()) {
                    walletService.createWalletForUser(user.getId());
                    created++;
                }
            }

            logger.info("Wallet backfill: {} missing wallets created.", created);
        };
    }
}
