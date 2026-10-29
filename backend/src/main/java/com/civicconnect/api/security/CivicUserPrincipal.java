package com.civicconnect.api.security;

import com.civicconnect.api.enums.UserRole;

/** Trusted identity built from the current persisted user, never request data. */
public record CivicUserPrincipal(Long userId, String email, UserRole role) { }
