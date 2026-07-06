package STARTER.Services.Interface;

import STARTER.DTOs.WalletViewDTO;

import java.util.UUID;

public interface WalletService {

    WalletViewDTO getWalletByUserId(UUID userId);
    void createWalletForUser(UUID userId);
    WalletViewDTO getWalletByUsername(String username);

}
