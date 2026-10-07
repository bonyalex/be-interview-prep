package com.example.beinterviewprep.dto;

public record TokenResponse(String token, String tokenType, long expiresInSeconds) {}
