package STARTER.Configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROFILES = "profiles";
    public static final String WALLET_SETTINGS = "walletSettings";
    public static final String TRANSACTION_HISTORY = "transactionHistory";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PROFILES, WALLET_SETTINGS, TRANSACTION_HISTORY);
    }
}
