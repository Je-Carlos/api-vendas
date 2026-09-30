package com.exemplo.authservice.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {
    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void save(String hash, long userId, Instant expires) {
        jdbc.update("INSERT INTO refresh_tokens (token_hash, user_id, expires_at) VALUES (?, ?, ?)",
                hash, userId, OffsetDateTime.ofInstant(expires, ZoneOffset.UTC));
    }

    public Optional<Long> consume(String hash) {
        return jdbc.query("UPDATE refresh_tokens SET revoked_at = now() "
                        + "WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > now() RETURNING user_id",
                (rs, row) -> rs.getLong(1), hash).stream().findFirst();
    }
}
