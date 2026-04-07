package com.appointunified.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AppointUnified API")
                .description("""
                    **AppointUnified V1 — Unified Smart Appointment Engine**
                    
                    Serving Healthcare · Government · Services from one platform.
                    
                    ## V1 New Features
                    - 🧠 **Smart Slot Recommendations** — Personalised provider/slot suggestions
                    - 😊 **Provider Mood Status** — Live availability context (AVAILABLE, RUNNING_LATE, etc.)
                    - 📝 **Booking Drafts** — Auto-save wizard progress, resume anytime
                    - 🔗 **Shareable Links + iCal** — Share appointments and download calendar files
                    - ⏰ **Proactive Reminders** — 24h, 2h, 30min automated email reminders
                    
                    ## Authentication
                    All protected routes require `Bearer <JWT>` in the `Authorization` header.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("AppointUnified")
                    .email("api@appointunified.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://appointunified.com/terms")))
            .servers(List.of(
                new Server().url("http://localhost:8080/api").description("Local development"),
                new Server().url("https://api.appointunified.com").description("Production")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
            .components(new Components()
                .addSecuritySchemes("Bearer Token", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter your JWT access token")));
    }
}
