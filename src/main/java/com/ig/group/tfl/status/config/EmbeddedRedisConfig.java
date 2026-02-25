package com.ig.group.tfl.status.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@Configuration
@Slf4j
public class EmbeddedRedisConfig {

    private final RedisServer redisServer;

    public EmbeddedRedisConfig(@Value("${spring.data.redis.port:6379}") int redisPort) {
        log.info("Initializing Embedded Redis Server on port {}", redisPort);
        this.redisServer = new RedisServer(redisPort);
    }

    @PostConstruct
    public void startRedis() {
        try {
            redisServer.start();
            log.info("Embedded Redis started successfully.");
        } catch (Exception e) {
            log.warn("Could not start embedded redis, it may already be running: {}", e.getMessage());
        }
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("lineStatus", "futureStatus", "unplannedDisruptions");
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
            log.info("Embedded Redis stopped successfully.");
        }
    }
}
