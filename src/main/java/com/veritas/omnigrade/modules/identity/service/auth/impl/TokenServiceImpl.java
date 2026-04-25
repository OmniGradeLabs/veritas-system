package com.veritas.omnigrade.modules.identity.service.auth.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.veritas.omnigrade.common.exception.ApiException;
import com.veritas.omnigrade.common.exception.ErrorCode;
import com.veritas.omnigrade.infrastructure.persistence.UuidV7;
import com.veritas.omnigrade.modules.identity.dto.auth.request.TokenRequest;
import com.veritas.omnigrade.modules.identity.dto.auth.response.TokenPair;
import com.veritas.omnigrade.modules.identity.service.auth.TokenService;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// subject        : user identifier (Student code / Username)
// univ_id        : tenant id by university (OmniGrade Core)
// user_id        : account UUID
// session_id     : session UUID (auth boundary)
// jti            : refresh token identifier (CAS rotation)
// typ=REFRESH    : mark refresh token explicitly
@Service
public class TokenServiceImpl implements TokenService {

  @Value("${jwt.signer-key-base64}")
  private String signerKeyBase64;

  @Value("${jwt.valid-duration}")
  private long accessTokenTtlSeconds;

  @Value("${jwt.refreshable-duration}")
  private long refreshTokenTtlSeconds;

  @Override
  public TokenPair generateTokenPair(TokenRequest tokenRequest) {
    String access = generateAccessToken(tokenRequest);
    String refresh = generateRefreshToken(tokenRequest);

    return new TokenPair(access, refresh, accessTokenTtlSeconds, refreshTokenTtlSeconds);
  }

  @Override
  public String generateAccessToken(TokenRequest tokenRequest) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.userId(),
            tokenRequest.universityId(),
            tokenRequest.sessionId(),
            tokenRequest.subject(),
            accessTokenTtlSeconds,
            null,
            null);
    return sign(claims);
  }

  @Override
  public String generateRefreshToken(TokenRequest tokenRequest) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.userId(),
            tokenRequest.universityId(),
            tokenRequest.sessionId(),
            tokenRequest.subject(),
            refreshTokenTtlSeconds,
            "REFRESH",
            tokenRequest.refreshJti());
    return sign(claims);
  }

  @Override
  public String generateRefreshToken(TokenRequest tokenRequest, Date absoluteExpiry) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.userId(),
            tokenRequest.universityId(),
            tokenRequest.sessionId(),
            tokenRequest.subject(),
            absoluteExpiry,
            "REFRESH",
            tokenRequest.refreshJti());
    return sign(claims);
  }

  @Override
  public SignedJWT verifyAccessToken(String token) {
    return verify(token, false);
  }

  @Override
  public SignedJWT verifyRefreshToken(String token) {
    return verify(token, true);
  }

  @Override
  public TokenRequest extractToken(String refreshToken) {
    SignedJWT jwt = verifyRefreshToken(refreshToken);
    try {
      var claims = jwt.getJWTClaimsSet();

      UUID userId = UUID.fromString(claims.getStringClaim("user_id"));
      UUID univId = UUID.fromString(claims.getStringClaim("univ_id"));
      UUID sessionId = UUID.fromString(claims.getStringClaim("session_id"));
      UUID jti = UUID.fromString(claims.getJWTID());
      String subject = claims.getSubject();

      if (subject == null || subject.isBlank()) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      return TokenRequest.builder()
          .userId(userId)
          .universityId(univId)
          .sessionId(sessionId)
          .subject(subject)
          .refreshJti(jti)
          .build();

    } catch (ParseException | IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private JWTClaimsSet buildClaims(
      UUID userId,
      UUID universityId,
      UUID sessionId,
      String subject,
      long ttlSeconds,
      String tokenType,
      UUID refreshJti) {

    Instant now = Instant.now();
    return buildClaims(
        userId,
        universityId,
        sessionId,
        subject,
        Date.from(now.plusSeconds(ttlSeconds)),
        tokenType,
        refreshJti);
  }

  private JWTClaimsSet buildClaims(
      UUID userId,
      UUID universityId,
      UUID sessionId,
      String subject,
      Date expirationTime,
      String tokenType,
      UUID refreshJti) {

    Instant now = Instant.now();

    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(Date.from(now))
            .expirationTime(expirationTime)
            .claim("user_id", userId.toString())
            .claim("univ_id", universityId.toString())
            .claim("session_id", sessionId.toString());

    if (tokenType != null) {
      builder.claim("typ", tokenType);
    }
    if ("REFRESH".equals(tokenType)) {
      if (refreshJti == null) {
        throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
      }
      builder.jwtID(refreshJti.toString());
    } else {
      builder.jwtID(UuidV7.random().toString());
    }

    return builder.build();
  }

  private String sign(JWTClaimsSet claims) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(signerKeyBase64);
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
      jwt.sign(new MACSigner(keyBytes));
      return jwt.serialize();
    } catch (Exception e) {
      throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
    }
  }

  private SignedJWT verify(String token, boolean refresh) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(signerKeyBase64);
      SignedJWT signedJWT = SignedJWT.parse(token);

      boolean signatureValid = signedJWT.verify(new MACVerifier(keyBytes));
      if (!signatureValid) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (exp == null || exp.before(new Date())) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      if (refresh) {
        Object typ = signedJWT.getJWTClaimsSet().getClaim("typ");
        if (!"REFRESH".equals(typ)) {
          throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        if (jti == null || jti.isBlank()) {
          throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
      }

      return signedJWT;

    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
