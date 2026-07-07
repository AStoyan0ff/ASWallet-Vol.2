package STARTER.Controllers;

import STARTER.DTOs.ProfileEditRequest;
import STARTER.DTOs.UserProfileDetailsViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Services.Interface.UserProfileDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileController.class)
class ProfileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileDetailsService userProfileDetailsService;

    private UserProfileDetailsViewDTO profileView;
    private ProfileEditRequest editRequest;

    @BeforeEach
    void setUp() {
        profileView = new UserProfileDetailsViewDTO();
        profileView.setId(UUID.randomUUID());
        profileView.setUsername("Plamen");
        profileView.setFirstName("Plamen");
        profileView.setLastName("Ivanov");
        profileView.setPhone("+359888123456");
        profileView.setEmail("plamen@example.com");
        profileView.setAvatarImageSrc("/images/default-avatar.svg");
        profileView.setAccountStatus(AccountStatus.ACTIVE);

        editRequest = new ProfileEditRequest();
        editRequest.setFirstName("Plamen");
        editRequest.setLastName("Ivanov");
        editRequest.setPhoneNumber("+359888123456");

        when(userProfileDetailsService.getProfileView("Plamen")).thenReturn(profileView);
        when(userProfileDetailsService.buildEditRequest("Plamen")).thenReturn(editRequest);
    }

    @Test
    void getProfile_returnsProfileViewWithData() throws Exception {
        mockMvc.perform(get("/profile").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("profile", profileView));
    }

    @Test
    void getEditProfile_returnsEditViewWithFormAndAvatar() throws Exception {
        mockMvc.perform(get("/profile/edit")
                        .with(csrf())
                        .with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attribute("profileEditRequest", editRequest))
                .andExpect(model().attribute("currentAvatarSrc", "/images/default-avatar.svg"));
    }

    @Test
    void postEditProfile_success_redirectsToProfileAndCallsService() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("firstName", "Plamen")
                        .param("lastName", "Petrov")
                        .param("phoneNumber", "+359888999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Profile updated successfully."));

        verify(userProfileDetailsService).updateProfile(eq("Plamen"), any(ProfileEditRequest.class), isNull());
    }

    @Test
    void postEditProfile_validationError_redirectsBackWithoutCallingService() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .with(user("Plamen"))
                        .param("firstName", "1invalid")
                        .param("lastName", "Ivanov")
                        .param("phoneNumber", "not-a-phone"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/edit"));

        verify(userProfileDetailsService, never()).updateProfile(any(), any(), any());
    }
}
