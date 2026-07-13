package STARTER.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LegalPagesController.class)
class LegalPagesControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void privacyPolicy_returnsView() throws Exception {
        mockMvc.perform(get("/wallet/privacy").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("privacy-policy"));
    }

    @Test
    void termsOfService_returnsView() throws Exception {
        mockMvc.perform(get("/wallet/terms").with(user("Plamen")))
                .andExpect(status().isOk())
                .andExpect(view().name("terms-of-service"));
    }
}
