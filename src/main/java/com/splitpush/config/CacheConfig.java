package com.splitpush.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "users",
            "userByUsername",
            "userByEmail",
            "tripGroups",
            "tripGroupById",
            "userGroups",
            "expenses",
            "expensesByGroup",
            "expenseById",
            "balances",
            "settlements",
            "settlementsByGroup",
            "settlementsByUser"
        );

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        caffeine.maximumSize(1000);
        caffeine.expireAfterWrite(5, TimeUnit.MINUTES);
        caffeine.expireAfterAccess(2, TimeUnit.MINUTES);
        caffeine.recordStats();
        cacheManager.setCaffeine(caffeine);

        return cacheManager;
    }
}

