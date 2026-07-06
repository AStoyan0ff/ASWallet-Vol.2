package STARTER.Configuration;

import STARTER.DTOs.BankCardRequest;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Services.Interface.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Configuration
public class AdminBootstrapConfig {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    @Transactional
    public ApplicationRunner adminBootstrapRunner(
            UserRepository userRepository,
            WalletService walletService,
            UserProfileDetailsService userProfileDetailsService,
            BankCardService bankCardService,
            PasswordEncoder passwordEncoder,

            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.email:aswallet.noreply@abv.bg}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.card.number:4111111111111111}") String adminCardNumber,
            @Value("${app.admin.card.holder:ASWallet Admin}") String adminCardHolder,
            @Value("${app.admin.card.expiry-month:12}") String adminCardExpiryMonth,
            @Value("${app.admin.card.expiry-year:30}") String adminCardExpiryYear,
            @Value("${app.admin.card.cvc:123}") String adminCardCvc) {

        BankCardRequest adminCardRequest = BankCardRequest.builder()
                .cardNumber(adminCardNumber)
                .cardholderName(adminCardHolder)
                .expiryMonth(adminCardExpiryMonth)
                .expiryYear(adminCardExpiryYear)
                .cardCvc(adminCardCvc)
                .build();

        return args -> {
            // Advanced backfill missing user profiles
            userProfileDetailsService.ensureProfileExistsForAllUsers();

            int corrected = 0;

            for (User user : userRepository.findAll()) {

                if (!adminUsername.equals(user.getUsername())) {
                    continue;
                }

                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepository.save(user);
                    corrected++;
                }
            }

            if (corrected > 0) {
                logger.info("Ensured primary admin '{}' has ADMIN role ({} correction(s)).", adminUsername, corrected);
            }

            Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

            if (existingAdmin.isPresent()) {
                User admin = existingAdmin.get();

                if (!adminEmail.equals(admin.getEmail())) {
                    admin.setEmail(adminEmail);
                    userRepository.save(admin);

                    logger.info("Updated admin email to {}", adminEmail);
                }

                ensureAdminBankCard(bankCardService, adminUsername, adminCardRequest);
                return;
            }

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .build();

            User savedAdmin = userRepository.save(admin);
            // Advanced — create admin profile
            userProfileDetailsService.createDefaultForUser(savedAdmin);

            walletService.createWalletForUser(savedAdmin.getId());
            ensureAdminBankCard(bankCardService, adminUsername, adminCardRequest);

            logger.info("Default admin account created: username={}", adminUsername);
        };
    }

    private void ensureAdminBankCard(
        BankCardService bankCardService,
        String adminUsername,
        BankCardRequest adminCardRequest) {

        if (bankCardService.getBankCardByUsername(adminUsername).isPresent()) {
            return;
        }

        bankCardService.saveBankCard(adminUsername, adminCardRequest);
        logger.info("Registered default bank card for admin '{}'", adminUsername);
    }
}
