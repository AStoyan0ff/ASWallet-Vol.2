package STARTER.Security;

import STARTER.Enums.AccountStatus;
import STARTER.Enums.UserRole;
import STARTER.Models.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean accountNonExpired = true;
    private final boolean accountNonLocked = true;
    private final boolean credentialsNonExpired = true;
    private final boolean enabled;

    public AppUserPrincipal(User user, AccountStatus accountStatus) {

        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = accountStatus == AccountStatus.ACTIVE;
        this.authorities = List.of(new SimpleGrantedAuthority(
                user.getRole() == UserRole.ADMIN
                        ? "ROLE_ADMIN"
                        : "ROLE_USER"
        ));
    }
}
