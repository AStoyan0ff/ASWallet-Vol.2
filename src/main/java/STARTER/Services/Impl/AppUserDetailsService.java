package STARTER.Services.Impl;

import STARTER.Enums.AccountStatus;
import STARTER.Models.User;
import STARTER.Models.UserProfileDetails;
import STARTER.Repositories.UserProfileDetailsRepository;
import STARTER.Repositories.UserRepository;
import STARTER.Security.AppUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserProfileDetailsRepository profileDetailsRepository;

    public AppUserDetailsService(UserRepository userRepository,UserProfileDetailsRepository profileDetailsRepository) {

        this.userRepository = userRepository;
        this.profileDetailsRepository = profileDetailsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
            new UsernameNotFoundException("User not found"));

        AccountStatus accountStatus = profileDetailsRepository.findByUser_Username(username)
            .map(UserProfileDetails::getAccountStatus)
            .orElse(AccountStatus.ACTIVE);

        return new AppUserPrincipal(user, accountStatus);
    }
}
