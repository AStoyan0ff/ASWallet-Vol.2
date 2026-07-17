package STARTER.Services.Interface;

import STARTER.DTOs.PaymentItemDTO;

import java.util.List;

public interface PaymentService {

    List<PaymentItemDTO> listPaymentsForUsername(String username);
}
