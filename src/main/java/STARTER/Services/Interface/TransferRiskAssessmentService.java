package STARTER.Services.Interface;

import STARTER.Clients.Dto.RiskAssessmentClientResponse;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.Models.User;
import STARTER.Models.Wallet;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRiskAssessmentService {

    RiskAssessmentClientResponse assessTransfer(
            User senderUser,
            Wallet senderWallet,
            User receiverUser,
            Wallet receiverWallet,
            TransferMoneyDTO transferMoneyDTO,
            boolean receiverHasBankCard
    );
}
