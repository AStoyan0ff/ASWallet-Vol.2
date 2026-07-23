package STARTER.Services.Impl;

import STARTER.Configuration.CacheConfig;
import STARTER.Models.Wallet;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApplicationCacheEviction {

    private final CacheManager cacheManager;
    public ApplicationCacheEviction(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictTransactionHistory(UUID userId) {

        if (userId == null) {
            return;
        }

        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTION_HISTORY);

        if (cache != null) {
            cache.evict(userId);
        }
    }

    public void evictTransactionHistoryForWallets(Wallet senderWallet, Wallet receiverWallet) {

        if (senderWallet != null && senderWallet.getUser() != null) {
            evictTransactionHistory(senderWallet.getUser().getId());
        }

        if (receiverWallet != null && receiverWallet.getUser() != null) {
            evictTransactionHistory(receiverWallet.getUser().getId());
        }
    }

    public void evictProfile(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        Cache cache = cacheManager.getCache(CacheConfig.PROFILES);

        if (cache != null) {
            cache.evict(username);
        }
    }

    public void evictWalletSettings(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        Cache cache = cacheManager.getCache(CacheConfig.WALLET_SETTINGS);

        if (cache != null) {
            cache.evict(username);
        }
    }
}
