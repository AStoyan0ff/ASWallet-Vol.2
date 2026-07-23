package STARTER.Services.Impl;

import STARTER.CustomException.UserNotFoundException;
import STARTER.CustomException.WalletNotFoundException;
import STARTER.DTOs.PaymentItemDTO;
import STARTER.Enums.TransactionStatus;
import STARTER.Enums.TransactionType;
import STARTER.Models.Transaction;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Models.Wallet;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final int AVATAR_TONE_COUNT = 6;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final String defaultAvatarImage;

    public PaymentServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            @Value("${app.avatar.default-image:/images/default-avatar.svg}") String defaultAvatarImage
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.defaultAvatarImage = defaultAvatarImage;
    }

    @Override
    public List<PaymentItemDTO> listPaymentsForUsername(String username) {
        User currentUser = userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUser_Id(currentUser.getId()).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        Specification<Transaction> outgoingTransfers = (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("senderWallet"), wallet),
                        criteriaBuilder.equal(root.get("type"), TransactionType.TRANSFER)
                );

        Map<String, UserProfileDetails> profileCache = new HashMap<>();

        return transactionRepository
                .findAll(outgoingTransfers, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(transaction -> toPaymentItem(transaction, profileCache))
                .toList();
    }

    private PaymentItemDTO toPaymentItem(
            Transaction transaction,
            Map<String, UserProfileDetails> profileCache) {

        User receiver = transaction.getReceiverWallet() != null
                ? transaction.getReceiverWallet().getUser()
                : null;

        String receiverUsername = receiver != null
            ? receiver.getUsername()
            : "Unknown";

        UserProfileDetails profile = profileCache.computeIfAbsent(
                receiverUsername,
                this::findProfileByUsername);

        String displayName = resolveDisplayName(profile, receiverUsername);

        boolean hasCustomAvatar = profile != null &&
                 profile.getAvatarUrl() != null &&
                !profile.getAvatarUrl().isBlank();

        return PaymentItemDTO.builder()
                .receiverUser(displayName)
                .avatarImageSrc(resolveAvatarImageSrc(profile != null
                    ? profile.getAvatarUrl()
                    : null))
                .hasCustomAvatar(hasCustomAvatar)
                .initials(resolveInitials(profile, receiverUsername))
                .avatarTone(Math.floorMod(receiverUsername.hashCode(), AVATAR_TONE_COUNT))
                .data(buildDataLine(transaction))
                .dateLabel(formatPaymentDate(transaction.getCreatedAt()))
                .build();
    }

    private UserProfileDetails findProfileByUsername(String username) {
        return profileDetailsRepository.findByUser_Username(username).orElse(null);
    }

    private String resolveDisplayName(UserProfileDetails profile, String username) {

        if (profile == null) {
            return username;
        }

        String first = normalize(profile.getFirstName());
        String last = normalize(profile.getLastName());

        if (first != null && last != null) {
            return first + " " + last;
        }

        if (first != null) {
            return first;
        }

        if (last != null) {
            return last;
        }

        return username;
    }

    private String resolveInitials(UserProfileDetails profile, String username) {

        String first = profile != null ? normalize(profile.getFirstName()) : null;
        String last = profile != null ? normalize(profile.getLastName()) : null;

        if (first != null && last != null) {
            return ("" + first.charAt(0) + last.charAt(0)).toUpperCase(Locale.ROOT);
        }

        if (first != null) {
            return first.substring(0, Math.min(2, first.length())).toUpperCase(Locale.ROOT);
        }

        String safe = username == null
            ? "?"
            : username.trim();

        if (safe.isEmpty()) {
            return "?";
        }

        return safe.substring(0, Math.min(2, safe.length())).toUpperCase(Locale.ROOT);
    }

    private String buildDataLine(Transaction transaction) {

        TransactionStatus status = transaction.getStatus();
        String amountLabel = formatAmount(transaction.getAmount());

        if (status == TransactionStatus.COMPLETED) {
            return "You sent " + amountLabel;
        }

        if (status == TransactionStatus.PENDING || status == TransactionStatus.PENDING_RISK_REVIEW) {
            return "Sending " + amountLabel;
        }

        if (status == TransactionStatus.FAILED) {
            return "Failed · " + amountLabel;
        }

        if (status == TransactionStatus.CANCELLED) {
            return "Cancelled · " + amountLabel;
        }

        return amountLabel;
    }

    private String formatAmount(BigDecimal amount) {

        BigDecimal safe = amount != null
            ? amount
            : BigDecimal.ZERO;

        return "€ " + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPaymentDate(LocalDateTime createdAt) {

        if (createdAt == null) {
            return "-";
        }

        int day = createdAt.getDayOfMonth();
        int month = createdAt.getMonthValue();

        if (createdAt.getYear() == Year.now().getValue()) {
            return day + "." + String.format("%02d", month);
        }

        return day + "." + String.format("%02d", month) + "." + createdAt.getYear();
    }

    private String resolveAvatarImageSrc(String avatarUrl) {

        if (avatarUrl == null || avatarUrl.isBlank()) {
            return defaultAvatarImage;
        }

        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") || avatarUrl.startsWith("/")) {
            return avatarUrl;
        }

        return "/" + avatarUrl;
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
            ? null
            : trimmed;
    }
}
