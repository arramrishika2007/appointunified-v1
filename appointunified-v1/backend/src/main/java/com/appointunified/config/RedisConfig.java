package com.appointunified.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * V3: Redis (Upstash) configuration.
 * Upstash free tier: 10,000 commands/day → batch carefully.
 * TTL strategy: queue state expires at midnight (end of queue day).
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value("${app.redis.url:${REDIS_URL:}}")
    private String redisUrl;

    @Value("${app.redis.password:}")
    private String redisPassword;

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public LettuceConnectionFactory redisConnectionFactory() {
        if (redisUrl == null || redisUrl.isBlank()) {
            log.warn("Redis URL is not configured. Falling back to localhost:6379 for development startup.");
            return new LettuceConnectionFactory("localhost", 6379);
        }
        // Upstash provides a rediss:// URL with TLS
        // Parse host/port/password from the URL
        try {
            java.net.URI uri = java.net.URI.create(redisUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 6379;
            boolean tls = redisUrl.startsWith("rediss://");

            var config = new org.springframework.data.redis.connection.RedisStandaloneConfiguration(host, port);
            if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                config.setPassword(uri.getUserInfo().split(":", 2)[1]);
            } else if (!redisPassword.isBlank()) {
                config.setPassword(redisPassword);
            }

            var clientConfig = org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.builder();
            if (tls) clientConfig.useSsl().disablePeerVerification();

            return new LettuceConnectionFactory(config, clientConfig.build());
        } catch (Exception e) {
            throw new RuntimeException("Invalid Redis URL: " + redisUrl, e);
        }
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
