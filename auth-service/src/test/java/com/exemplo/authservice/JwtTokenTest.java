package com.exemplo.authservice;

import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.service.JwtToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenTest {
    @Test
    void accessTokenHasSubjectRoleAndFiniteLifetime() {
        String secret = "01234567890123456789012345678901";
        Usuario user = new Usuario("Ana", "ana@example.com", "hash");
        String token = new JwtToken(secret).gerarToken(user);
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();

        assertEquals("ana@example.com", claims.getSubject());
        assertEquals("USER", claims.get("roles"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertEquals(900, claims.getExpiration().toInstant().getEpochSecond()
                - claims.getIssuedAt().toInstant().getEpochSecond());
    }
}
