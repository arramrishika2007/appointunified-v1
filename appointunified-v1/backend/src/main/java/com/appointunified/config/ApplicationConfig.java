package com.appointunified.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {
    
    /**
     * RestTemplate bean for external API calls (Groq, etc.)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
