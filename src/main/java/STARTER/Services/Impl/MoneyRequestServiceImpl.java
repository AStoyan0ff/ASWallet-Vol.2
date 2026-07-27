package STARTER.Services.Impl;

import STARTER.CustomException.MoneyRequestActionNotAllowedException;
import STARTER.CustomException.MoneyRequestNotFoundException;
import STARTER.CustomException.NotTransferMoneyYourselfException;
import STARTER.CustomException.UserNotFoundException;
import STARTER.DTOs.MoneyRequestCreateDTO;
import STARTER.DTOs.MoneyRequestViewDTO;
import STARTER.DTOs.TransferMoneyDTO;
import STARTER.Enums.MoneyRequestStatus;
import STARTER.Models.MoneyRequest;
import STARTER.Models.User;
import STARTER.Repositories.MoneyRequestRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Services.Interface.MoneyRequestService;
import STARTER.Services.Interface.TransactionService;
import STARTER.Utils.DateTimeDisplay;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MoneyRequestServiceImpl implements MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public MoneyRequestServiceImpl(
            MoneyRequestRepository moneyRequestRepository,
            UserRepository userRepository,
            TransactionService transactionService
    ) {
        this.moneyRequestRepository = moneyRequestRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional
    public void create(String requesterUsername, MoneyRequestCreateDTO dto) {
        User requester = findUser(requesterUsername);
        User payer = userRepository.findByUsername(dto.getPayerUsername().trim())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + dto.getPayerUsername()));

        if (requester.getId().equals(payer.getId())) {
            throw new NotTransferMoneyYourselfException("You cannot request money from yourself.");
        }

        String note = dto.getNote() == null ? null : dto.getNote().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }

        MoneyRequest request = MoneyRequest.builder()
                .requester(requester)
                .payer(payer)
                .amount(dto.getAmount())
                .spendingCategory(dto.getSpendingCategory())
                .note(note)
                .status(MoneyRequestStatus.PENDING)
                .build();

        moneyRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyRequestViewDTO> listIncoming(String username) {
        User user = findUser(username);
        return moneyRequestRepository.findByPayer_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(request -> toView(request, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyRequestViewDTO> listOutgoing(String username) {
        User user = findUser(username);
        return moneyRequestRepository.findByRequester_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(request -> toView(request, false))
                .toList();
    }

    @Override
    @Transactional
    public void accept(UUID requestId, String payerUsername) {
        MoneyRequest request = findPendingForPayer(requestId, payerUsername);

        TransferMoneyDTO transfer = new TransferMoneyDTO();
        transfer.setReceiverUsername(request.getRequester().getUsername());
        transfer.setAmount(request.getAmount());
        transfer.setSpendingCategory(request.getSpendingCategory());

        transactionService.transfer(request.getPayer().getId(), transfer);

        request.setStatus(MoneyRequestStatus.ACCEPTED);
        request.setResolvedAt(LocalDateTime.now());
        moneyRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void decline(UUID requestId, String payerUsername) {
        MoneyRequest request = findPendingForPayer(requestId, payerUsername);
        request.setStatus(MoneyRequestStatus.DECLINED);
        request.setResolvedAt(LocalDateTime.now());
        moneyRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void cancel(UUID requestId, String requesterUsername) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new MoneyRequestNotFoundException("Money request not found."));

        if (!request.getRequester().getUsername().equals(requesterUsername)) {
            throw new MoneyRequestActionNotAllowedException("You can only cancel your own money requests.");
        }

        if (request.getStatus() != MoneyRequestStatus.PENDING) {
            throw new MoneyRequestActionNotAllowedException("Only pending money requests can be cancelled.");
        }

        request.setStatus(MoneyRequestStatus.CANCELLED);
        request.setResolvedAt(LocalDateTime.now());
        moneyRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingIncoming(String payerUsername) {
        return moneyRequestRepository.countByPayer_UsernameAndStatus(payerUsername, MoneyRequestStatus.PENDING);
    }

    @Override
    @Transactional
    public int deleteAllForUser(String username) {
        User user = findUser(username);
        return moneyRequestRepository.deleteAllForUser(user.getId());
    }

    private MoneyRequest findPendingForPayer(UUID requestId, String payerUsername) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new MoneyRequestNotFoundException("Money request not found."));

        if (!request.getPayer().getUsername().equals(payerUsername)) {
            throw new MoneyRequestActionNotAllowedException("You can only respond to money requests sent to you.");
        }

        if (request.getStatus() != MoneyRequestStatus.PENDING) {
            throw new MoneyRequestActionNotAllowedException("This money request is no longer pending.");
        }

        return request;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private MoneyRequestViewDTO toView(MoneyRequest request, boolean incoming) {
        boolean pending = request.getStatus() == MoneyRequestStatus.PENDING;

        return MoneyRequestViewDTO.builder()
                .id(request.getId())
                .requesterUsername(request.getRequester().getUsername())
                .payerUsername(request.getPayer().getUsername())
                .amount(request.getAmount())
                .spendingCategory(request.getSpendingCategory().getLabel())
                .note(request.getNote())
                .status(request.getStatus().name())
                .createdAt(DateTimeDisplay.format(request.getCreatedAt()))
                .resolvedAt(DateTimeDisplay.format(request.getResolvedAt()))
                .incoming(incoming)
                .canRespond(incoming && pending)
                .canCancel(!incoming && pending)
                .build();
    }
}
