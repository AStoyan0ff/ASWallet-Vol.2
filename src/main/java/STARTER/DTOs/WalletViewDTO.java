package STARTER.DTOs;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor

public class WalletViewDTO {

    private UUID id;
    private BigDecimal balance;
    private String currency;
    private UUID userId;
    private String username;
}
