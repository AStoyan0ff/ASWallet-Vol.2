package STARTER.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContactUsController {

    @GetMapping("/wallet/contact-us")
    public String contactUsPage() {
        return "contact-us";
    }
}
