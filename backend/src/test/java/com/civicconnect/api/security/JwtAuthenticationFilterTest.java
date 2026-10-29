package com.civicconnect.api.security;

import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-must-be-at-least-thirty-two-bytes-long";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsingPersistedUserInsteadOfTokenRoleClaim() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3600);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setEmail("citizen@example.com");
        user.setRole(UserRole.CITIZEN);
        user.setEnabled(true);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.createAccessToken("citizen@example.com"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CITIZEN"));
    }

    @Test
    void rejectsInvalidBearerToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET, 3600), mock(UserRepository.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer malformed-token");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
