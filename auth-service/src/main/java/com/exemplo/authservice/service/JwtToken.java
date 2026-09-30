package com.exemplo.authservice.service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.exemplo.authservice.model.Usuario;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtToken {

    private  SecretKey secretKey;
    @Value("${ACCESS_TOKEN_TTL_SECONDS:900}")
    private long ttlSeconds = 900;

    public JwtToken(@Value("${JWT_SECRET}") String segredo) {
        secretKey = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Usuario usuario){
        Instant issued = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuedAt(Date.from(issued))
                .expiration(Date.from(issued.plusSeconds(ttlSeconds)))
                .claim("roles", "USER")
                .signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    public long expiresIn() { return ttlSeconds; }
    
}
