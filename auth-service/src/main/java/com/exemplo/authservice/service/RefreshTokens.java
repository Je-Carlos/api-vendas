package com.exemplo.authservice.service;

import com.exemplo.authservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokens {
    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final long ttlSeconds;

    public RefreshTokens(RefreshTokenRepository repository,
                         @Value("${REFRESH_TOKEN_TTL_SECONDS:604800}") long ttlSeconds) {
        this.repository = repository;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(hash(token), userId, Instant.now().plusSeconds(ttlSeconds));
        return token;
    }

    public Optional<Long> consume(String token) { return repository.consume(hash(token)); }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
