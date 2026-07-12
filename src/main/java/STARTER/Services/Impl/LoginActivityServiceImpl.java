package STARTER.Services.Impl;

import STARTER.DTOs.LoginActivityViewDTO;
import STARTER.Models.LoginActivity;
import STARTER.Repositories.LoginActivityRepository;
import STARTER.Services.Interface.LoginActivityService;
import STARTER.Utils.ClientIpUtils;
import STARTER.Utils.DateTimeDisplay;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoginActivityServiceImpl implements LoginActivityService {

    private static final Logger logger = LoggerFactory.getLogger(LoginActivityServiceImpl.class);
    private final LoginActivityRepository loginActivityRepository;

    public LoginActivityServiceImpl(LoginActivityRepository loginActivityRepository) {
        this.loginActivityRepository = loginActivityRepository;
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String username, HttpServletRequest request) {
        String ipAddress = ClientIpUtils.resolve(request);

        LoginActivity activity = LoginActivity.builder()
                .username(username)
                .ipAddress(ipAddress)
                .build();

        loginActivityRepository.save(activity);

        logger.info("Login recorded: username={}, ip={}", username, ipAddress);
    }

    @Override
    public List<LoginActivityViewDTO> getLastLogins(int limit) {
        int safeLimit = Math.clamp(limit, 1, 50); //  Math.max(1, Math.min(limit, 50)

        return loginActivityRepository.findAllByOrderByLoggedInAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToView)
                .toList();
    }

    @Override
    @Transactional
    public int clearAll() {
        long count = loginActivityRepository.count();

        if (count == 0) {
            return 0;
        }

        loginActivityRepository.deleteAllInBatch();
        logger.info("Cleared {} login activity record(s)", count);

        return (int) count;
    }

    private LoginActivityViewDTO mapToView(LoginActivity activity) {
        return LoginActivityViewDTO.builder()
                .username(activity.getUsername())
                .ipAddress(ClientIpUtils.normalize(activity.getIpAddress()))
                .loggedInAt(DateTimeDisplay.format(activity.getLoggedInAt()))
                .build();
    }
}
