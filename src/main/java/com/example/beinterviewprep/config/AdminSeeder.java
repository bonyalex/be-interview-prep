package com.example.beinterviewprep.config;

import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.repository.AppUserRepository;
import com.example.beinterviewprep.service.AuthService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AdminProperties properties;
    private final AppUserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }
        if (userRepository.existsByEmail(properties.email().trim().toLowerCase(Locale.ROOT))) {
            return;
        }
        authService.create(properties.email(), properties.password(), Role.ADMIN);
        log.info("Seeded admin account");
    }
}
