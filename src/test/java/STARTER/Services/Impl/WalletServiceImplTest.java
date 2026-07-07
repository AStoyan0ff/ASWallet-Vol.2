package STARTER.Services.Impl;

import STARTER.CustomException.UserNotFoundException;
import STARTER.CustomException.WalletNotFoundException;
import STARTER.DTOs.WalletViewDTO;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User user;
    private Wallet wallet;
    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        user = User.builder()
                .username("Plamen")
                .email("plamen@example.com")
                .password("encoded")
                .build();
        user.setId(userId);

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("150.50"));
        wallet.setCurrency("EUR");
    }

    // --- GET BY USER ID ---

    @Test
    void getWalletByUserId_success_mapsToWalletViewDTO() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        WalletViewDTO result = walletService.getWalletByUserId(userId);

        assertThat(result.getId()).isEqualTo(walletId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("Plamen");
        assertThat(result.getBalance()).isEqualByComparingTo("150.50");
        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void getWalletByUserId_walletNotFound_throwsWalletNotFoundException() {
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.getWalletByUserId(userId));
    }

    // --- GET BY USERNAME ---

    @Test
    void getWalletByUsername_success_mapsToWalletViewDTO() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        WalletViewDTO result = walletService.getWalletByUsername("Plamen");

        assertThat(result.getId()).isEqualTo(walletId);
        assertThat(result.getUsername()).isEqualTo("Plamen");
        assertThat(result.getBalance()).isEqualByComparingTo("150.50");
        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void getWalletByUsername_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("Missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> walletService.getWalletByUsername("Missing"));

        verify(walletRepository, never()).findByUser_Id(any());
    }

    @Test
    void getWalletByUsername_walletNotFound_throwsWalletNotFoundException() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.getWalletByUsername("Plamen"));
    }

    // --- CREATE ---

    @Test
    void createWalletForUser_success_savesWalletWithZeroBalanceAndEur() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        walletService.createWalletForUser(userId);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());

        Wallet saved = walletCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void createWalletForUser_userNotFound_throwsAndSavesNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> walletService.createWalletForUser(userId));

        verify(walletRepository, never()).save(any());
    }
}
