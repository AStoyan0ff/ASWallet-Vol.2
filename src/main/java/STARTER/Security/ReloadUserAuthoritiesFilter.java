package STARTER.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Advanced — reload roles from DB so promoted admins work without re-login
@Component
public class ReloadUserAuthoritiesFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    public ReloadUserAuthoritiesFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            UserDetails updated = userDetailsService.loadUserByUsername(principal.getUsername());

            UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                    updated,
                    authentication.getCredentials(),
                    updated.getAuthorities()
            );

            refreshed.setDetails(authentication.getDetails());
            SecurityContextHolder.getContext().setAuthentication(refreshed);
        }

        filterChain.doFilter(request, response);
    }
}
