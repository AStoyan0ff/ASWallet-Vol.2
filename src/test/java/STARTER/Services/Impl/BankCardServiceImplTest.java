package STARTER.Services.Impl;

import STARTER.CustomException.InvalidCardDetailsException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.BankCardRequest;
import STARTER.DTOs.BankCardViewDTO;
import STARTER.Models.BankCard;
import STARTER.Models.User;
import STARTER.Repositories.BankCardRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankCardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private BankCardRepository bankCardRepository;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private BankCardServiceImpl bankCardService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .username("Plamen")
                .email("plamen@example.com")
                .password("encoded")
                .build();
        user.setId(userId);
    }

    private BankCardRequest validRequest() {

        return BankCardRequest.builder()
                .cardNumber("4111 1111 1111 1111")
                .cardholderName("Plamen Test")
                .expiryMonth("12")
                .expiryYear("30")
                .cardCvc("123")
                .build();
    }

    // --- GET ---

    @Test
    void getBankCardByUsername_found_mapsMaskedCardAndFormattedIban() {
        BankCard bankCard = BankCard.builder()
                .user(user)
                .lastFourDigits("1111")
                .cardholderName("Plamen Test")
                .expiryMonth("12")
                .expiryYear("30")
                .iban("BG18ASWL12345678901234")
                .build();

        when(bankCardRepository.findByUser_Username("Plamen")).thenReturn(Optional.of(bankCard));

        Optional<BankCardViewDTO> result = bankCardService.getBankCardByUsername("Plamen");

        assertThat(result).isPresent();
        assertThat(result.get().getMaskedCardNumber()).isEqualTo("**** **** **** 1111");
        assertThat(result.get().getLastFourDigits()).isEqualTo("1111");
        assertThat(result.get().getCardholderName()).isEqualTo("Plamen Test");
        assertThat(result.get().getFormattedIban()).isNotBlank();
        assertThat(result.get().getIban()).isEqualTo("BG18ASWL12345678901234");
    }

    @Test
    void getBankCardByUsername_notFound_returnsEmpty() {
        when(bankCardRepository.findByUser_Username("Missing")).thenReturn(Optional.empty());

        Optional<BankCardViewDTO> result = bankCardService.getBankCardByUsername("Missing");

        assertThat(result).isEmpty();
    }

    // --- SAVE ---

    @Test
    void saveBankCard_firstCard_savesDetails_grantsWelcomeBonus_returnsTrue() {
        BankCardRequest request = validRequest();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(bankCardRepository.findByUser_Username("Plamen"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(bankCardRepository.existsByIban(any())).thenReturn(false);

        boolean bonusGranted = bankCardService.saveBankCard("Plamen", request);

        assertThat(bonusGranted).isTrue();

        ArgumentCaptor<BankCard> cardCaptor = ArgumentCaptor.forClass(BankCard.class);
        verify(bankCardRepository).save(cardCaptor.capture());

        BankCard saved = cardCaptor.getValue();
        assertThat(saved.getLastFourDigits()).isEqualTo("1111");
        assertThat(saved.getCardholderName()).isEqualTo("Plamen Test");
        assertThat(saved.getExpiryMonth()).isEqualTo("12");
        assertThat(saved.getExpiryYear()).isEqualTo("30");
        assertThat(saved.getIban()).isNotBlank();
        assertThat(saved.getUser()).isEqualTo(user);

        verify(transactionService).grantWelcomeBonus(userId);
    }

    @Test
    void saveBankCard_updateExisting_keepsIban_skipsWelcomeBonus_returnsFalse() {
        BankCard existing = BankCard.builder()
                .user(user)
                .lastFourDigits("4242")
                .cardholderName("Old Name")
                .expiryMonth("01")
                .expiryYear("28")
                .iban("BG18ASWL99999999999999")
                .build();

        BankCardRequest request = BankCardRequest.builder()
                .cardNumber("5500000000000004")
                .cardholderName("  Plamen Updated  ")
                .expiryMonth("06")
                .expiryYear("31")
                .cardCvc("456")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(bankCardRepository.findByUser_Username("Plamen"))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(existing));

        boolean bonusGranted = bankCardService.saveBankCard("Plamen", request);

        assertThat(bonusGranted).isFalse();

        ArgumentCaptor<BankCard> cardCaptor = ArgumentCaptor.forClass(BankCard.class);
        verify(bankCardRepository).save(cardCaptor.capture());

        BankCard saved = cardCaptor.getValue();
        assertThat(saved.getLastFourDigits()).isEqualTo("0004");
        assertThat(saved.getCardholderName()).isEqualTo("Plamen Updated");
        assertThat(saved.getIban()).isEqualTo("BG18ASWL99999999999999");

        verify(transactionService, never()).grantWelcomeBonus(any());
        verify(bankCardRepository, never()).existsByIban(any());
    }

    @Test
    void saveBankCard_userNotFound_throwsAndSavesNothing() {
        when(userRepository.findByUsername("Missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> bankCardService.saveBankCard("Missing", validRequest()));

        verify(bankCardRepository, never()).save(any());
        verify(transactionService, never()).grantWelcomeBonus(any());
    }

    @Test
    void saveBankCard_expiredCard_throwsInvalidCardDetailsException() {
        BankCardRequest request = BankCardRequest.builder()
                .cardNumber("4111111111111111")
                .cardholderName("Plamen Test")
                .expiryMonth("01")
                .expiryYear("20")
                .cardCvc("123")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(bankCardRepository.findByUser_Username("Plamen")).thenReturn(Optional.empty());

        assertThrows(InvalidCardDetailsException.class,
                () -> bankCardService.saveBankCard("Plamen", request));

        verify(bankCardRepository, never()).save(any());
        verify(transactionService, never()).grantWelcomeBonus(any());
    }

    @Test
    void saveBankCard_stripsNonDigitsFromCardNumber_beforeSavingLastFour() {
        BankCardRequest request = BankCardRequest.builder()
                .cardNumber("4111-1111-1111-2222")
                .cardholderName("Plamen Test")
                .expiryMonth("12")
                .expiryYear("30")
                .cardCvc("123")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(bankCardRepository.findByUser_Username("Plamen"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(bankCardRepository.existsByIban(any())).thenReturn(false);

        bankCardService.saveBankCard("Plamen", request);

        ArgumentCaptor<BankCard> cardCaptor = ArgumentCaptor.forClass(BankCard.class);
        verify(bankCardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getLastFourDigits()).isEqualTo("2222");
    }
}
