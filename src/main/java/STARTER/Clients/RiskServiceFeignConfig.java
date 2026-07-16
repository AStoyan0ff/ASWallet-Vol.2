package STARTER.Clients;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class RiskServiceFeignConfig {

    @Bean
    public RequestInterceptor riskServiceApiKeyInterceptor(
                @Value("${app.risk-service.api-key}") String apiKey) {

        return template -> template.header("X-API-Key", apiKey);
    }
}
