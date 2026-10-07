package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.beinterviewprep.config.JwtProperties;
import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.exception.AuthenticationFailedException;
import com.example.beinterviewprep.service.JwtService.TokenPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String PLACEHOLDER_KEY = "placeholder-key-for-tests-0123456789-abcdef";
    private static final String OTHER_PLACEHOLDER_KEY = "other-placeholder-key-0123456789-uvwxyz";
    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final JwtProperties properties = new JwtProperties(PLACEHOLDER_KEY, Duration.ofMinutes(15));

    private JwtService serviceAt(Instant now) {
        return new JwtService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private AppUser user(Role role) {
        AppUser user = new AppUser();
        user.setEmail("jane@example.com");
        user.setRole(role);
        return user;
    }

    @Test
    void tokenCarriesEmailAndRole() {
        String token = serviceAt(ISSUED_AT).generate(user(Role.ADMIN));

        TokenPrincipal principal = serviceAt(ISSUED_AT).parse(token);

        assertEquals("jane@example.com", principal.email());
        assertEquals(Role.ADMIN, principal.role());
    }

    @Test
    void tokenIsAcceptedJustBeforeFifteenMinutes() {
        String token = serviceAt(ISSUED_AT).generate(user(Role.USER));

        TokenPrincipal principal = serviceAt(ISSUED_AT.plus(Duration.ofMinutes(14).plusSeconds(59)))
                .parse(token);

        assertEquals(Role.USER, principal.role());
    }

    @Test
    void tokenIsRejectedAfterFifteenMinutes() {
        String token = serviceAt(ISSUED_AT).generate(user(Role.USER));
        JwtService later = serviceAt(ISSUED_AT.plus(Duration.ofMinutes(15).plusSeconds(1)));

        assertThrows(AuthenticationFailedException.class, () -> later.parse(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = serviceAt(ISSUED_AT).generate(user(Role.ADMIN));
        JwtProperties other = new JwtProperties(OTHER_PLACEHOLDER_KEY, Duration.ofMinutes(15));
        JwtService otherService = new JwtService(other, Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

        assertThrows(AuthenticationFailedException.class, () -> otherService.parse(token));
    }

    @Test
    void garbageTokenIsRejected() {
        assertThrows(AuthenticationFailedException.class, () -> serviceAt(ISSUED_AT).parse("not-a-token"));
    }
}
