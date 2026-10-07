package com.example.beinterviewprep.service;

import com.example.beinterviewprep.config.JwtProperties;
import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.exception.AuthenticationFailedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
        this.clock = clock;
    }

    public String generate(AppUser user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiry())))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return properties.expiry().toSeconds();
    }

    public TokenPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenPrincipal(claims.getSubject(), Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Invalid or expired token");
        }
    }

    public record TokenPrincipal(String email, Role role) {}
}
