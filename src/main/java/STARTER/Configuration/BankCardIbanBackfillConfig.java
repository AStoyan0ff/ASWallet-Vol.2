package STARTER.Configuration;

import STARTER.Models.BankCard;
import STARTER.Repositories.BankCardRepository;
import STARTER.Utils.IbanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class BankCardIbanBackfillConfig {

    private static final Logger logger = LoggerFactory.getLogger(BankCardIbanBackfillConfig.class);

    @Bean
    @Transactional
    public ApplicationRunner missingBankCardIbanBackfillRunner(BankCardRepository bankCardRepository) {
        return args -> {
            int updated = 0;

            for (BankCard bankCard : bankCardRepository.findAll()) {
                if (bankCard.getIban() != null && !bankCard.getIban().isBlank()) {
                    continue;
                }

                bankCard.setIban(IbanGenerator.generate(
                        bankCard.getUser().getId(),
                        candidate -> !bankCardRepository.existsByIban(candidate)
                ));
                bankCardRepository.save(bankCard);
                updated++;
            }

            if (updated > 0) {
                logger.info("Bank card IBAN backfill: {} cards updated.", updated);
            }
        };
    }
}
