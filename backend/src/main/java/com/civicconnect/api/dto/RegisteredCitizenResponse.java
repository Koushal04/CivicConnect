package com.civicconnect.api.dto;

import com.civicconnect.api.enums.UserRole;

public record RegisteredCitizenResponse(Long id, String fullName, String email, UserRole role) { }
