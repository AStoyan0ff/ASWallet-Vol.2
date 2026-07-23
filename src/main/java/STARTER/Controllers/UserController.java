package STARTER.Controllers;

import STARTER.DTOs.LoginDTO;
import STARTER.DTOs.UserDTO;
import STARTER.Services.Interface.LoginActivityService;
import STARTER.Services.Interface.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LoginActivityService loginActivityService;

    public UserController(
            UserService userService,
            AuthenticationManager authenticationManager,
            LoginActivityService loginActivityService
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.loginActivityService = loginActivityService;
    }

    @ModelAttribute("userDTO")
    public UserDTO createUser(UserDTO userDTO) {

        return userDTO != null
                ? userDTO
                : new UserDTO();
    }

    @GetMapping("/register")
    public String register(Model model) {

        if (!model.containsAttribute("userDTO")) {
            model.addAttribute("userDTO", new UserDTO());
        }

        return "register";
    }

    @PostMapping("/register")
    public String registerConfirm(@Valid @ModelAttribute("userDTO") UserDTO userDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("userDTO", userDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDTO", bindingResult);

            return "redirect:/register";
        }

        userService.register(userDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Account created successfully. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {

        if (!model.containsAttribute("loginDTO")) {
            model.addAttribute("loginDTO", new LoginDTO());
        }

        return "login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginDTO") LoginDTO loginDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute("loginDTO", loginDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.loginDTO", bindingResult);

            return "redirect:/login";
        }

        try {
            Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(

                    loginDTO.getUsername(),
                    loginDTO.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            loginActivityService.recordSuccessfulLogin(loginDTO.getUsername(), request);

            return "redirect:/wallet";

        } catch (BadCredentialsException ex) {

            loginDTO.setPassword(null);

            redirectAttributes.addFlashAttribute("loginDTO", loginDTO);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid username or password. Please try again."
            );

            return "redirect:/login";

        } catch (DisabledException ex) {

            loginDTO.setPassword(null);
            redirectAttributes.addFlashAttribute("loginDTO", loginDTO);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Your account is inactive. Please contact an administrator."
            );

            return "redirect:/login";
        }
    }
}
