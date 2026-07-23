package STARTER.Controllers;

import STARTER.DTOs.ProfileEditRequest;
import STARTER.Services.Interface.UserProfileDetailsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ProfileController {
    private final UserProfileDetailsService userProfileDetailsService;

    public ProfileController(UserProfileDetailsService userProfileDetailsService) {
        this.userProfileDetailsService = userProfileDetailsService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {

        model.addAttribute("profile", userProfileDetailsService.getProfileView(principal.getName()));
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, Principal principal) {

        if (!model.containsAttribute("profileEditRequest")) {
            model.addAttribute("profileEditRequest", userProfileDetailsService.buildEditRequest(principal.getName()));
        }

        model.addAttribute("currentAvatarSrc",
            userProfileDetailsService.getProfileView(principal.getName()).getAvatarImageSrc()
        );

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @Valid @ModelAttribute("profileEditRequest") ProfileEditRequest profileEditRequest,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Principal principal,
            RedirectAttributes redirectAttributes) {


        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileEditRequest", profileEditRequest);

            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileEditRequest",
                bindingResult
            );


            return "redirect:/profile/edit";
        }

        userProfileDetailsService.updateProfile(principal.getName(), profileEditRequest, avatarFile);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");

        return "redirect:/profile";
    }
}

