package com.example.gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"JWT_SECRET=01234567890123456789012345678901", "eureka.client.enabled=false"})
class GatewayApplicationTests {
	@Autowired WebTestClient http;

	@Test
	void protectedRouteRejectsMissingAndInvalidTokenAsJson() {
		http.get().uri("/produtos-service/produtos").exchange().expectStatus().isUnauthorized()
				.expectHeader().contentType("application/json")
				.expectBody().jsonPath("$.error").isEqualTo("Unauthorized");
		http.get().uri("/produtos-service/produtos").headers(h -> h.setBearerAuth("invalid"))
				.exchange().expectStatus().isUnauthorized()
				.expectHeader().contentType("application/json")
				.expectBody().jsonPath("$.error").isEqualTo("Unauthorized");
	}

	@Test
	void expiredTokenCannotReachProtectedRoute() {
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
				"01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
		String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder().subject("ana@example.com")
						.issuedAt(Instant.now().minusSeconds(1800))
						.expiresAt(Instant.now().minusSeconds(120)).build())).getTokenValue();
		http.get().uri("/produtos-service/produtos").headers(h -> h.setBearerAuth(expired))
				.exchange().expectStatus().isUnauthorized();
	}

}
