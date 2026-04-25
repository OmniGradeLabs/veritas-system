package com.veritas.omnigrade.modules.identity.dto.auth.response;

public record TokenPair(String accessToken, String refreshToken, long accessTtl, long refreshTtl) {}
