package STARTER.Services.Interface;

import STARTER.DTOs.MoneyRequestCreateDTO;
import STARTER.DTOs.MoneyRequestViewDTO;

import java.util.List;
import java.util.UUID;

public interface MoneyRequestService {

    void create(String requesterUsername, MoneyRequestCreateDTO dto);

    List<MoneyRequestViewDTO> listIncoming(String username);

    List<MoneyRequestViewDTO> listOutgoing(String username);

    void accept(UUID requestId, String payerUsername);

    void decline(UUID requestId, String payerUsername);

    void cancel(UUID requestId, String requesterUsername);

    long countPendingIncoming(String payerUsername);

    int deleteAllForUser(String username);
}
