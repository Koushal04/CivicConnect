package com.civicconnect.api.security;

import com.civicconnect.api.entity.User;
import com.civicconnect.api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("mysql")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(authorization.substring(7));
            String email = claims.getSubject();
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(email)
                        .filter(User::isEnabled)
                        .orElse(null);
                if (user == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
                    return;
                }
                authenticate(request, user);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, User user) {
        CivicUserPrincipal principal = new CivicUserPrincipal(user.getId(), user.getEmail(), user.getRole());
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
