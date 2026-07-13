package STARTER.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalPagesController {

    @GetMapping("/wallet/privacy")
    public String privacyPolicyPage() {
        return "privacy-policy";
    }

    @GetMapping("/wallet/terms")
    public String termsOfServicePage() {
        return "terms-of-service";
    }
}
