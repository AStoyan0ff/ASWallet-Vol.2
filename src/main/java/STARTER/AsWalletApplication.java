package STARTER;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ASWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(ASWalletApplication.class, args);
    }
}
