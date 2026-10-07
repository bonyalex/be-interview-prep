package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.LoginRequest;
import com.example.beinterviewprep.dto.RegisterRequest;
import com.example.beinterviewprep.dto.TokenResponse;
import com.example.beinterviewprep.dto.UserResponse;
import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.exception.AuthenticationFailedException;
import com.example.beinterviewprep.exception.BusinessRuleException;
import com.example.beinterviewprep.repository.AppUserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER = "Bearer";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return UserResponse.from(create(request.email(), request.password(), Role.USER));
    }

    @Transactional
    public AppUser create(String email, String password, Role role) {
        String normalized = normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new BusinessRuleException("Email already registered");
        }
        AppUser user = new AppUser();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        AppUser user = userRepository
                .findByEmail(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
        return new TokenResponse(jwtService.generate(user), BEARER, jwtService.expiresInSeconds());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
