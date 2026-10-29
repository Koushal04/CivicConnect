package com.civicconnect.api.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) { }
