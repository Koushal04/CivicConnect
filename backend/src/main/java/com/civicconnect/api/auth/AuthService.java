package com.civicconnect.api.auth;

import com.civicconnect.api.dto.LoginRequest;
import com.civicconnect.api.dto.LoginResponse;
import com.civicconnect.api.dto.RegisterCitizenRequest;
import com.civicconnect.api.dto.RegisteredCitizenResponse;
import com.civicconnect.api.entity.User;
import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.UserRepository;
import com.civicconnect.api.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("mysql")
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisteredCitizenResponse registerCitizen(RegisterCitizenRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CITIZEN);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        return new RegisteredCitizenResponse(
                savedUser.getId(), savedUser.getFullName(), savedUser.getEmail(), savedUser.getRole());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new LoginResponse(jwtService.createAccessToken(user.getEmail()), "Bearer", jwtService.getExpirationSeconds());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
