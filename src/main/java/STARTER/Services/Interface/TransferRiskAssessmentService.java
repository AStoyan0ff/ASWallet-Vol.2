package STARTER.Services.Interface;

import STARTER.Clients.DTO.RiskAssessmentClientResponse;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.Models.User;
import STARTER.Models.Wallet;

import java.util.UUID;

public interface TransferRiskAssessmentService {

    RiskAssessmentClientResponse assessTransfer(
            UUID transactionRef,
            User senderUser,
            Wallet senderWallet,
            User receiverUser,
            Wallet receiverWallet,
            TransferMoneyDTO transferMoneyDTO,
            boolean receiverHasBankCard
    );
}
