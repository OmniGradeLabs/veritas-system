package com.veritas.omnigrade.modules.identity.service.auth;

import com.nimbusds.jwt.SignedJWT;
import com.veritas.omnigrade.modules.identity.dto.auth.request.TokenRequest;
import com.veritas.omnigrade.modules.identity.dto.auth.response.TokenPair;
import java.util.Date;

public interface TokenService {
  TokenPair generateTokenPair(TokenRequest tokenRequest);

  String generateAccessToken(TokenRequest tokenRequest);

  String generateRefreshToken(TokenRequest tokenRequest);

  SignedJWT verifyAccessToken(String token);

  SignedJWT verifyRefreshToken(String token);

  TokenRequest extractToken(String refreshToken);

  String generateRefreshToken(TokenRequest tokenRequest, Date absoluteExpiry);
}
