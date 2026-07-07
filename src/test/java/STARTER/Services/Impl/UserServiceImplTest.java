package STARTER.Services.Impl;

import STARTER.CustomException.EmailAlreadyExistsException;
import STARTER.CustomException.PasswordMismatchException;
import STARTER.CustomException.UserAlreadyExistsException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.DeleteAccountRequest;
import STARTER.DTOs.UserDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.Enums.UserRole;
import STARTER.Events.UserRegisteredEvent;
import STARTER.Models.User;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Services.Interface.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private UserDeletionService userDeletionService;
    @Mock private UserProfileDetailsService userProfileDetailsService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .username("Plamen")
                .email("plamen@example.com")
                .password("encoded-old")
                .role(UserRole.USER)
                .build();
        user.setId(userId);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
    }

    // --- REGISTER ---

    @Test
    void register_success_createsUserWalletProfile_andPublishesEvent() {
        UserDTO dto = UserDTO.builder()
                .username("NewUser")
                .email("new@example.com")
                .password("Secret1!")
                .confirmPassword("Secret1!")
                .build();

        when(userRepository.existsByUsername("NewUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {

            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        userService.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("NewUser");
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-secret");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);

        verify(userProfileDetailsService).createDefaultForUser(savedUser);
        verify(walletService).createWalletForUser(savedUser.getId());

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().email()).isEqualTo("new@example.com");
        assertThat(eventCaptor.getValue().username()).isEqualTo("NewUser");
    }

    @Test
    void register_passwordMismatch_throwsAndSavesNothing() {
        UserDTO dto = UserDTO.builder()
                .username("NewUser")
                .email("new@example.com")
                .password("Secret1!")
                .confirmPassword("Different1!")
                .build();

        assertThrows(PasswordMismatchException.class, () -> userService.register(dto));

        verify(userRepository, never()).save(any());
        verify(walletService, never()).createWalletForUser(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void register_usernameExists_throwsUserAlreadyExistsException() {

        UserDTO dto = UserDTO.builder()
                .username("Taken")
                .email("new@example.com")
                .password("Secret1!")
                .confirmPassword("Secret1!")
                .build();

        when(userRepository.existsByUsername("Taken")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailExists_throwsEmailAlreadyExistsException() {

        UserDTO dto = UserDTO.builder()
                .username("NewUser")
                .email("taken@example.com")
                .password("Secret1!")
                .confirmPassword("Secret1!")
                .build();

        when(userRepository.existsByUsername("NewUser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any());
    }

    // --- FIND ---

    @Test
    void findByUsername_success_mapsToUserViewDTO() {
        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));

        UserViewDTO result = userService.findByUsername("Plamen");

        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("Plamen");
        assertThat(result.getEmail()).isEqualTo("plamen@example.com");
        assertThat(result.getRegisteredAt()).isEqualTo("2026-01-15 10:00:00");
    }

    @Test
    void findByUsername_notFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("Missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findByUsername("Missing"));
    }

    // --- CHANGE PASSWORD ---

    @Test
    void changePassword_success_encodesAndSavesNewPassword() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass1!")
                .newPassword("NewPass1!")
                .confirmPassword("NewPass1!")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded-new");

        userService.changePassword("Plamen", request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPassword_throwsUserNotFoundException() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongPass1!")
                .newPassword("NewPass1!")
                .confirmPassword("NewPass1!")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "encoded-old")).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> userService.changePassword("Plamen", request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrent_throwsPasswordMismatchException() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass1!")
                .newPassword("OldPass1!")
                .confirmPassword("OldPass1!")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encoded-old")).thenReturn(true);

        assertThrows(PasswordMismatchException.class,
                () -> userService.changePassword("Plamen", request));

        verify(userRepository, never()).save(any());
    }

    // --- DELETE ACCOUNT ---

    @Test
    void deleteAccount_success_delegatesToUserDeletionService() {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .password("Secret1!")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret1!", "encoded-old")).thenReturn(true);

        userService.deleteAccount("Plamen", request);

        verify(userDeletionService).deleteUserFully(user);
    }

    @Test
    void deleteAccount_wrongPassword_throwsPasswordMismatchException() {

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .password("WrongPass1!")
                .build();

        when(userRepository.findByUsername("Plamen")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "encoded-old")).thenReturn(false);

        assertThrows(PasswordMismatchException.class,
                () -> userService.deleteAccount("Plamen", request));

        verify(userDeletionService, never()).deleteUserFully(any());
    }
}
