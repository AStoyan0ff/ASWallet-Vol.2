package STARTER.Services;

import STARTER.Clients.RiskAssessmentClient;
import STARTER.DTOs.BankCardRequest;
import STARTER.DTOs.UserDTO;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.BankCardRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserProfileDetailsRepository profileDetailsRepository;

    @Autowired
    private BankCardRepository bankCardRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private RiskAssessmentClient riskAssessmentClient;

    @Test
    void register_persistsUserProfileAndWallet() {
        UserDTO request = UserDTO.builder()
                .username("IntegUser")
                .email("integ.user@example.com")
                .password("Passw0rd!")
                .confirmPassword("Passw0rd!")
                .build();

        userService.register(request);

        User saved = userRepository.findByUsername("IntegUser").orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("integ.user@example.com");
        assertThat(saved.getPassword()).isNotEqualTo("Passw0rd!");

        assertThat(profileDetailsRepository.findByUser_Id(saved.getId())).isPresent();
        assertThat(walletRepository.findByUser_Id(saved.getId()))
                .isPresent()
                .get()
                .satisfies(wallet -> {
                    assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(wallet.getCurrency()).isEqualTo("EUR");
                });
    }

    @Test
    void saveBankCard_firstCard_persistsCardAndGrantsWelcomeBonus() {
        userService.register(UserDTO.builder()
                .username("CardUser")
                .email("card.user@example.com")
                .password("Passw0rd!")
                .confirmPassword("Passw0rd!")
                .build());

        User saved = userRepository.findByUsername("CardUser").orElseThrow();

        boolean bonusGranted = bankCardService.saveBankCard(
                "CardUser",
                BankCardRequest.builder()
                        .cardNumber("4111111111111111")
                        .cardholderName("Card User")
                        .expiryMonth("12")
                        .expiryYear("30")
                        .cardCvc("123")
                        .build()
        );

        assertThat(bonusGranted).isTrue();
        assertThat(bankCardRepository.findByUser_Username("CardUser"))
                .isPresent()
                .get()
                .satisfies(card -> {
                    assertThat(card.getLastFourDigits()).isEqualTo("1111");
                    assertThat(card.getIban()).isNotBlank();
                });

        assertThat(walletRepository.findByUser_Id(saved.getId()))
                .isPresent()
                .get()
                .extracting(Wallet::getBalance)
                .satisfies(balance -> assertThat((BigDecimal) balance)
                .isEqualByComparingTo(new BigDecimal("50.00")));

    }
}
