package com.appointunified;

import com.appointunified.dto.request.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbXVzdC1iZS1sb25nLWVub3VnaA==",
    "app.jwt.access-token-expiry-ms=900000",
    "app.jwt.refresh-token-expiry-ms=2592000000",
    "app.firebase.credentials={}",
    "app.firebase.project-id=test",
    "spring.mail.host=localhost",
    "spring.mail.port=1025"
})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signUp_validRequest_returns201WithTokens() throws Exception {
        var request = new AuthRequest.SignUp();
        request.setPhone("+919876543210");
        request.setFullName("Test User");
        request.setPassword("password123");
        request.setRole("PUBLIC");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.user.phone").value("+919876543210"))
            .andExpect(jsonPath("$.data.user.role").value("PUBLIC"));
    }

    @Test
    void signUp_duplicatePhone_returns409() throws Exception {
        var request = new AuthRequest.SignUp();
        request.setPhone("+919999999999");
        request.setFullName("Duplicate Test");
        request.setPassword("password123");

        // First signup - should succeed
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Second signup same phone - should conflict
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void signUp_invalidPhone_returns400() throws Exception {
        var request = new AuthRequest.SignUp();
        request.setPhone("not-a-phone");
        request.setFullName("Bad User");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_afterSignup_returnsTokens() throws Exception {
        // Register first
        var signUp = new AuthRequest.SignUp();
        signUp.setPhone("+911234567890");
        signUp.setFullName("Login Test");
        signUp.setPassword("mypassword");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUp)))
            .andExpect(status().isCreated());

        // Now login
        var login = new AuthRequest.Login();
        login.setIdentifier("+911234567890");
        login.setPassword("mypassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var signUp = new AuthRequest.SignUp();
        signUp.setPhone("+910000000001");
        signUp.setFullName("Wrong Pass");
        signUp.setPassword("correct123");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUp)))
            .andExpect(status().isCreated());

        var login = new AuthRequest.Login();
        login.setIdentifier("+910000000001");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }
}
