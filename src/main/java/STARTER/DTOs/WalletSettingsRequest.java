package STARTER.DTOs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WalletSettingsRequest {

    private boolean balanceHidden;
    private boolean emailOnDeposit;
    private boolean emailOnWithdraw;
    private boolean emailOnTransfer;
}
