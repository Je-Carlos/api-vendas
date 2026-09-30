package com.exemplo.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "JWT_SECRET=01234567890123456789012345678901"})
class AuthFlowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registerLoginRotateAndRejectReplay() {
        var user = Map.of("nome", "Ana", "email", "ana@example.com", "senha", "secret123");
        assertEquals(HttpStatus.CREATED, http.postForEntity("/api/auth/register", user, Long.class).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, http.postForEntity("/api/auth/register", user, Map.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, http.postForEntity("/api/auth/register",
                Map.of("nome", "", "email", "wrong", "senha", "123"), Map.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, http.postForEntity("/api/auth/register",
                Map.of("nome", "A".repeat(256), "email", "other@example.com", "senha", "secret123"),
                Map.class).getStatusCode());
        String oversizedUtf8Password = "ç".repeat(40);
        assertEquals(HttpStatus.BAD_REQUEST, http.postForEntity("/api/auth/register",
                Map.of("nome", "Bia", "email", "bia@example.com", "senha", oversizedUtf8Password),
                String.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, http.postForEntity("/api/auth/login",
                Map.of("email", "ana@example.com", "senha", oversizedUtf8Password),
                Map.class).getStatusCode());
        String storedPassword = jdbc.queryForObject("SELECT senha_hash FROM users WHERE email = ?",
                String.class, "ana@example.com");
        assertNotEquals("secret123", storedPassword);
        assertTrue(storedPassword.startsWith("$2"));
        assertEquals(HttpStatus.UNAUTHORIZED, http.postForEntity("/api/auth/login",
                Map.of("email", "ana@example.com", "senha", "wrong"), Map.class).getStatusCode());

        var login = http.postForEntity("/api/auth/login",
                Map.of("email", "ana@example.com", "senha", "secret123"), Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertEquals("Bearer", login.getBody().get("tokenType"));
        assertEquals(900, login.getBody().get("expiresIn"));
        assertNotNull(login.getBody().get("accessToken"));
        assertFalse(login.getBody().containsKey("senha"));
        String oldRefresh = (String) login.getBody().get("refreshToken");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class, oldRefresh));
        var rotated = http.postForEntity("/api/auth/refresh", Map.of("refreshToken", oldRefresh), Map.class);
        assertEquals(HttpStatus.OK, rotated.getStatusCode());
        assertNotEquals(oldRefresh, rotated.getBody().get("refreshToken"));
        assertEquals(HttpStatus.UNAUTHORIZED, http.postForEntity("/api/auth/refresh",
                Map.of("refreshToken", oldRefresh), Map.class).getStatusCode());
    }
}
