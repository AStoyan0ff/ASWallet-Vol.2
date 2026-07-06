package STARTER.Services.Impl;

import STARTER.Models.Transaction;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.AdminMailboxMessageRepository;
import STARTER.Repositories.BankCardRepository;
import STARTER.Repositories.PasswordResetTokenRepository;
import STARTER.Repositories.TransactionRepository;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletionService.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BankCardRepository bankCardRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;
    private final AdminMailboxMessageRepository mailboxMessageRepository;
    private final UserRepository userRepository;

    public UserDeletionService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            BankCardRepository bankCardRepository,
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            UserProfileDetailsRepository profileDetailsRepository,
            AdminMailboxMessageRepository mailboxMessageRepository,
            UserRepository userRepository
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.bankCardRepository = bankCardRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.profileDetailsRepository = profileDetailsRepository;
        this.mailboxMessageRepository = mailboxMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void deleteUserFully(User user) {
        logger.info("Deleting user data: username={}, userId={}", user.getUsername(), user.getId());

        passwordResetTokenRepository.deleteByUser(user);
        bankCardRepository.deleteByUser(user);
        mailboxMessageRepository.deleteByRecipientUserId(user.getId());

        Wallet wallet = walletRepository.findByUser_Id(user.getId()).orElse(null);

        if (wallet != null) {
            List<Transaction> transactions = transactionRepository.findAllBySenderWalletOrReceiverWallet(wallet, wallet);

            transactionRepository.deleteAll(transactions);
            walletRepository.delete(wallet);
            user.setWallet(null);
            userRepository.saveAndFlush(user);
        }

        // Advanced - delete profile details on user removal
        profileDetailsRepository.deleteByUser_Id(user.getId());
        user.setProfileDetails(null);
        userRepository.delete(user);
    }
}
