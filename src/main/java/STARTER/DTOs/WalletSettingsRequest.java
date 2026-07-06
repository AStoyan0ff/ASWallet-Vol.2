package STARTER.DTOs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Advanced — wallet settings form (/wallet/settings)
@Getter
@Setter
@NoArgsConstructor
public class WalletSettingsRequest {

    private boolean balanceHidden;
    private boolean emailOnDeposit;
    private boolean emailOnWithdraw;
    private boolean emailOnTransfer;
}
