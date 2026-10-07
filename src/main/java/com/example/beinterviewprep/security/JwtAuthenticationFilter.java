package com.example.beinterviewprep.security;

import com.example.beinterviewprep.exception.AuthenticationFailedException;
import com.example.beinterviewprep.service.JwtService;
import com.example.beinterviewprep.service.JwtService.TokenPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            TokenPrincipal principal = jwtService.parse(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal.email(), null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationFailedException ex) {
            SecurityContextHolder.clearContext();
        }
    }
}
