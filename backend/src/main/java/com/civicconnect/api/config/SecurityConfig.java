
package com.civicconnect.api.config;

import com.civicconnect.api.security.JwtAuthenticationFilter;
import com.civicconnect.api.repository.UserRepository;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:5173}")
            String allowedOrigin) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("mysql")
    UserDetailsService databaseUserDetailsService(
            UserRepository userRepository) {

        return email -> userRepository.findByEmail(email)
                .map(user ->
                        org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPasswordHash())
                                .authorities("ROLE_" + user.getRole().name())
                                .disabled(!user.isEnabled())
                                .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }

    @Bean
    @Profile("local")
    UserDetailsService noUsersConfiguredYet() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Authentication is not configured in the local health profile");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationFilter>
                    jwtAuthenticationFilterProvider)
            throws Exception {

        http
            .cors(cors -> cors.configurationSource(
                    corsConfigurationSource("http://localhost:5173")))

            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/grievances/**",
                    "/api/admin/**",
                    "/api/officers/**"))

            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(authorize -> authorize

                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                        HttpMethod.GET, "/api/health").permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login").permitAll()

                // Allow logged-in users to access their own account details.
                .requestMatchers("/api/auth/me").authenticated()

                // Keep role-based authorization.
                .requestMatchers("/api/grievances/**")
                    .hasRole("CITIZEN")

                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                .requestMatchers("/api/officers/**")
                    .hasRole("OFFICER")

                .anyRequest().denyAll()
            );

        JwtAuthenticationFilter jwtAuthenticationFilter =
                jwtAuthenticationFilterProvider.getIfAvailable();

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
