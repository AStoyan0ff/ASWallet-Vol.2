package STARTER.Services.Impl;

import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.DeleteAccountRequest;
import STARTER.DTOs.UserDTO;
import STARTER.DTOs.UserViewDTO;
import STARTER.CustomException.EmailAlreadyExistsException;
import STARTER.CustomException.PasswordMismatchException;
import STARTER.CustomException.UserAlreadyExistsException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import STARTER.Repositories.UserRepository;
import STARTER.Events.UserRegisteredEvent;
import STARTER.Services.Interface.UserProfileDetailsService;
import STARTER.Services.Interface.UserService;
import STARTER.Services.Interface.WalletService;
import STARTER.Utils.DateTimeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final UserDeletionService userDeletionService;
    private final UserProfileDetailsService userProfileDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;


    public UserServiceImpl(
            UserRepository userRepository,
            WalletService walletService,
            UserDeletionService userDeletionService,
            UserProfileDetailsService userProfileDetailsService,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.userDeletionService = userDeletionService;
        this.userProfileDetailsService = userProfileDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }


    @Override
    @Transactional
    public void register(UserDTO userDTO) {

        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Username or Password not matches");
        }

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .role(UserRole.USER)
                .build();

        // Advanced - create default profile on register
        User savedUser = userRepository.save(user);

        userProfileDetailsService.createDefaultForUser(savedUser);
        walletService.createWalletForUser(savedUser.getId());
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getEmail(), savedUser.getUsername()));
        logger.info("User registered: username={}, userId={}", savedUser.getUsername(), savedUser.getId());
    }

    @Override
    public UserViewDTO findById(UUID id) {

        User user = userRepository.findById(id).orElseThrow(() ->
            new UserNotFoundException("User not found"));

        return mapToEntity(user);
    }

    @Override
    public UserViewDTO findByUsername(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
            new UserNotFoundException("User not found"));

        return mapToEntity(user);
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UserNotFoundException("Old password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("New password and confirm new password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new PasswordMismatchException("New password must be different from your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed: username={}", username);
    }

    @Override
    @Transactional
    public void deleteAccount(String username, DeleteAccountRequest request) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new PasswordMismatchException("Current password is incorrect");
        }

        userDeletionService.deleteUserFully(user);
        logger.info("Account deleted by user: username={}, userId={}", username, user.getId());
    }

    private UserViewDTO mapToEntity(User user) {
        UserViewDTO uv = new UserViewDTO();

        uv.setId(user.getId());
        uv.setUsername(user.getUsername());
        uv.setEmail(user.getEmail());
        uv.setRegisteredAt(DateTimeDisplay.format(user.getCreatedAt()));

        return uv;
    }
}
