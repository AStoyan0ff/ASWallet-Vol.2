package STARTER.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppModelAdvice {

    @Value("${app.version}")
    private String appVersion;

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersion;
    }
}
