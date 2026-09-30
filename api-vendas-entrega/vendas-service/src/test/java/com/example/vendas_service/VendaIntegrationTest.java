package com.example.vendas_service;

import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "JWT_SECRET=01234567890123456789012345678901"})
class VendaIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static HttpServer products = startProducts();

    static HttpServer startProducts() {
        try {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/produtos/1", exchange -> {
            byte[] body = "{\"id\":1,\"nome\":\"Caneca\",\"preco\":12.5}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/produtos/2", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.createContext("/produtos/3", exchange -> {
            byte[] body = "{\"id\":null,\"nome\":\"Invalido\",\"preco\":null}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/produtos/4", exchange -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        return server;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @AfterAll static void stopProducts() { products.stop(0); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName());
        properties.add("spring.r2dbc.username", postgres::getUsername);
        properties.add("spring.r2dbc.password", postgres::getPassword);
        properties.add("spring.sql.init.mode", () -> "always");
        properties.add("produtos.base-url", () -> "http://localhost:" + products.getAddress().getPort());
        properties.add("produtos.timeout-seconds", () -> "1");
    }

    @Autowired WebTestClient http;
    @Autowired com.example.vendas_service.repository.VendasRepository repository;

    @Test
    void authenticatedSalePersistsReactively() {
        http.post().uri("/api/vendas").bodyValue(Map.of("idProduto", 1, "quantidade", 2))
                .exchange().expectStatus().isUnauthorized();
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        String jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject("ana@example.com").issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(900)).build())).getTokenValue();
        http.post().uri("/api/vendas").headers(h -> h.setBearerAuth(jwt))
                .bodyValue(Map.of("idProduto", 1, "quantidade", 2)).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.preco").isEqualTo(12.5);
        http.post().uri("/api/vendas").headers(h -> h.setBearerAuth(jwt))
                .bodyValue(Map.of("idProduto", 999, "quantidade", 2)).exchange()
                .expectStatus().isNotFound();
        http.post().uri("/api/vendas").headers(h -> h.setBearerAuth(jwt))
                .bodyValue(Map.of("idProduto", 2, "quantidade", 2)).exchange()
                .expectStatus().isEqualTo(502);
        http.post().uri("/api/vendas").headers(h -> h.setBearerAuth(jwt))
                .bodyValue(Map.of("idProduto", 3, "quantidade", 2)).exchange()
                .expectStatus().isEqualTo(502);
        http.post().uri("/api/vendas").headers(h -> h.setBearerAuth(jwt))
                .bodyValue(Map.of("idProduto", 4, "quantidade", 2)).exchange()
                .expectStatus().isEqualTo(502);
        StepVerifier.create(repository.findAll()).assertNext(v -> {
            assertEquals(1L, v.getIdProduto());
            assertEquals(2, v.getQuantidade());
        }).verifyComplete();
    }
}
