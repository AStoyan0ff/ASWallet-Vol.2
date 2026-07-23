package STARTER.Models;

import STARTER.Enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "user_profile_details")
public class UserProfileDetails extends BaseClass {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "balance_hidden", nullable = false)
    @Builder.Default
    private boolean balanceHidden = false;

    @Column(name = "email_on_deposit", nullable = false)
    @Builder.Default
    private boolean emailOnDeposit = true;

    @Column(name = "email_on_withdraw", nullable = false)
    @Builder.Default
    private boolean emailOnWithdraw = true;

    @Column(name = "email_on_transfer", nullable = false)
    @Builder.Default
    private boolean emailOnTransfer = true;

    @Column(name = "daily_withdraw_limit")
    private BigDecimal dailyWithdrawLimit;

    @Override
    protected void onPrePersist() {

        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }
}
