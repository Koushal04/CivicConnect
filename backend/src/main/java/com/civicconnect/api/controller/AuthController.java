
package com.civicconnect.api.controller;

import com.civicconnect.api.auth.AuthService;
import com.civicconnect.api.dto.LoginRequest;
import com.civicconnect.api.dto.LoginResponse;
import com.civicconnect.api.dto.RegisterCitizenRequest;
import com.civicconnect.api.dto.RegisteredCitizenResponse;
import com.civicconnect.api.security.CivicUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredCitizenResponse> register(
            @Valid @RequestBody RegisterCitizenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerCitizen(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(
            @AuthenticationPrincipal CivicUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(java.util.Map.of(
                "id", principal.userId(),
                "email", principal.email(),
                "role", principal.role().name()
        ));
    }
}
