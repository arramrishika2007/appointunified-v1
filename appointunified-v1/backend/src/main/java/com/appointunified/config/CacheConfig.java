package com.appointunified.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${REDIS_URL:}')")
    public LettuceConnectionFactory redisConnectionFactory(@Value("${REDIS_URL}") String redisUrl) {
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(uri.getHost());
        configuration.setPort(uri.getPort() > 0 ? uri.getPort() : 6379);

        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            String password = userInfo.substring(userInfo.indexOf(':') + 1);
            configuration.setPassword(RedisPassword.of(password));
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.setUseSsl("rediss".equalsIgnoreCase(uri.getScheme()));
        log.info("Redis cache enabled for host {}", uri.getHost());
        return factory;
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${REDIS_URL:}')")
    public CacheManager redisCacheManager(LettuceConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${REDIS_URL:}')")
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager("slotRecommendations");
    }
}