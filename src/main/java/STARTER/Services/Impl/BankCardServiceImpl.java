package STARTER.Services.Impl;

import STARTER.CustomException.InvalidCardDetailsException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.BankCardRequest;
import STARTER.DTOs.BankCardViewDTO;
import STARTER.Models.BankCard;
import STARTER.Models.User;
import STARTER.Repositories.BankCardRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.BankCardService;
import STARTER.Services.Interface.TransactionService;
import STARTER.Utils.CardValidationUtils;
import STARTER.Utils.IbanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BankCardServiceImpl implements BankCardService {

    private static final Logger logger = LoggerFactory.getLogger(BankCardServiceImpl.class);

    private final UserRepository userRepository;
    private final BankCardRepository bankCardRepository;
    private final TransactionService transactionService;

    public BankCardServiceImpl(
            UserRepository userRepository,
            BankCardRepository bankCardRepository,
            TransactionService transactionService) {

        this.userRepository = userRepository;
        this.bankCardRepository = bankCardRepository;
        this.transactionService = transactionService;
    }

    @Override
    public Optional<BankCardViewDTO> getBankCardByUsername(String username) {
        return bankCardRepository.findByUser_Username(username).map(this::mapToView);
    }

    @Override
    @Transactional
    public boolean saveBankCard(String username, BankCardRequest request) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        boolean isFirstCard = bankCardRepository.findByUser_Username(username).isEmpty();

        String digits = request.getCardNumber().replaceAll("\\D", "");

        if (!CardValidationUtils.isExpiryInFuture(request.getExpiryMonth(), request.getExpiryYear())) {
            throw new InvalidCardDetailsException("Card expiry date must be in the future");
        }

        String lastFour = digits.substring(digits.length() - 4);

        BankCard bankCard = bankCardRepository.findByUser_Username(username)
                .orElseGet(() -> BankCard.builder().user(user).build());

        bankCard.setLastFourDigits(lastFour);
        bankCard.setCardholderName(request.getCardholderName().trim());
        bankCard.setExpiryMonth(request.getExpiryMonth());
        bankCard.setExpiryYear(request.getExpiryYear());

        if (bankCard.getIban() == null || bankCard.getIban().isBlank()) {
            bankCard.setIban(IbanGenerator.generate(
                    user.getId(),
                    candidate -> !bankCardRepository.existsByIban(candidate)
            ));
        }

        bankCardRepository.save(bankCard);

        if (isFirstCard) {

            transactionService.grantWelcomeBonus(user.getId());
            logger.info("Bank card registered with welcome bonus: user={}, last4={}", username, lastFour);
            return true;
        }

        logger.info("Bank card updated: user={}, last4={}", username, lastFour);
        return false;
    }

    private BankCardViewDTO mapToView(BankCard bankCard) {

        return BankCardViewDTO.builder()
            .maskedCardNumber("**** **** **** " + bankCard.getLastFourDigits())
            .lastFourDigits(bankCard.getLastFourDigits())
            .cardholderName(bankCard.getCardholderName())
            .expiryMonth(bankCard.getExpiryMonth())
            .expiryYear(bankCard.getExpiryYear())
            .iban(bankCard.getIban())
            .formattedIban(bankCard.getIban() != null
                    ? IbanGenerator.formatForDisplay(bankCard.getIban())
                    : null)
            .build();
    }
}
