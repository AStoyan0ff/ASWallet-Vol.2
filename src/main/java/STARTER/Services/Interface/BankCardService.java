package STARTER.Services.Interface;

import STARTER.DTOs.BankCardRequest;
import STARTER.DTOs.BankCardViewDTO;

import java.util.Optional;

public interface BankCardService {

    Optional<BankCardViewDTO> getBankCardByUsername(String username);
    boolean saveBankCard(String username, BankCardRequest request);
}
