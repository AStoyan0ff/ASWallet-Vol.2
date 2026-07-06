package STARTER.Services.Impl;

import STARTER.DTOs.WalletViewDTO;
import STARTER.CustomException.UserNotFoundException;
import STARTER.CustomException.WalletNotFoundException;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import STARTER.Repositories.UserRepository;
import STARTER.Repositories.WalletRepository;
import STARTER.Services.Interface.WalletService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletServiceImpl(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Override
    public WalletViewDTO getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUser_Id(userId).orElseThrow(() ->
                new WalletNotFoundException("Wallet not found"));

        return mapToEntity(wallet);
    }

    @Override
    public void createWalletForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("EUR");

        walletRepository.save(wallet);
    }

    @Override
    public WalletViewDTO getWalletByUsername(String username) {

        User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Wallet wallet = this.walletRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return mapToEntity(wallet);
    }

    private WalletViewDTO mapToEntity(Wallet wallet) {
        WalletViewDTO dto = new WalletViewDTO();

        dto.setId(wallet.getId());
        dto.setBalance(wallet.getBalance());
        dto.setCurrency(wallet.getCurrency());
        dto.setUserId(wallet.getUser().getId());
        dto.setUsername(wallet.getUser().getUsername());

        return dto;
    }
}
