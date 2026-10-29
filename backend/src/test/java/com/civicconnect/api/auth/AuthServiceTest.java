package com.civicconnect.api.auth;

import com.civicconnect.api.dto.LoginRequest;
import com.civicconnect.api.dto.RegisterCitizenRequest;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = "test-secret-must-be-at-least-thirty-two-bytes-long";

    @Mock
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(JWT_SECRET, 3600);

    @Test
    void registersCitizenWithBCryptHashAndServerAssignedRole() {
        when(userRepository.existsByEmail("citizen@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);

        var response = service.registerCitizen(new RegisterCitizenRequest(
                "  Ada Citizen  ", "CITIZEN@example.com", "+91 9876543210", "secure-password"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CITIZEN);
        assertThat(savedUser.getEmail()).isEqualTo("citizen@example.com");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("secure-password");
        assertThat(passwordEncoder.matches("secure-password", savedUser.getPasswordHash())).isTrue();
        assertThat(response.role()).isEqualTo(UserRole.CITIZEN);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("citizen@example.com")).thenReturn(true);
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);

        assertThatThrownBy(() -> service.registerCitizen(new RegisterCitizenRequest(
                "Ada Citizen", "citizen@example.com", null, "secure-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void logsInWithValidCredentialsAndReturnsSignedToken() {
        User user = user("citizen@example.com", "secure-password", true);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);

        var response = service.login(new LoginRequest("CITIZEN@example.com", "secure-password"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(jwtService.parseClaims(response.accessToken()).getSubject()).isEqualTo("citizen@example.com");
    }

    @Test
    void rejectsInvalidCredentials() {
        User user = user("citizen@example.com", "secure-password", true);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);

        assertThatThrownBy(() -> service.login(new LoginRequest("citizen@example.com", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private User user(String email, String rawPassword, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.CITIZEN);
        user.setEnabled(enabled);
        return user;
    }
}
