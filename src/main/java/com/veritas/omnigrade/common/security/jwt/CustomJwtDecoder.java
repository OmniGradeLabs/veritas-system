package com.veritas.omnigrade.common.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomJwtDecoder implements JwtDecoder {
  NimbusJwtDecoder nimbusJwtDecoder;

  @Value("${jwt.signer-key-base64}")
  String signerKeyBase64;

  // TODO: Dùng PostConstruct khởi tạo Nimbus trước sẽ Thread-Safe hơn

  /**
   * Entry point of JWT decoding process. The token arrives here directly from the HTTP
   * Authorization header.
   */
  @Override
  public Jwt decode(String token) {
    try {
      // 1. Fast-fail Cryptographic & Expiry Validation
      // Check signature and expiry explicitly before letting Nimbus process it.
      // This prevents expensive parsing overhead if the token is obviously tampered with.
      validateTokenDirectly(token);
    } catch (JwtException e) {
      throw e;
    } catch (Exception e) {
      throw new JwtException("Unauthenticated", e);
    }

    // 2. Spring Security needs a 'Jwt' object representation to continue its flow.
    if (Objects.isNull(nimbusJwtDecoder)) {
      byte[] signerKeyBytes = Base64.getDecoder().decode(signerKeyBase64);

      SecretKeySpec secretKeySpec = new SecretKeySpec(signerKeyBytes, "HmacSHA512");

      nimbusJwtDecoder =
          NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
    }

    // 3. Decode into standard Spring Security 'Jwt' object.
    // --> NEXT STOP: This Jwt object will be sent to JwtAuthenticationConverter.
    return nimbusJwtDecoder.decode(token);
  }

  /**
   * Performs a strict verification of the JWT signature and expiration date. NOTE: This does NOT
   * check if the session is revoked in Redis/DB. Session validation is handled downstream in the
   * converter/resolver.
   */
  private void validateTokenDirectly(String token) throws JOSEException, ParseException {
    byte[] signerKeyBytes = Base64.getDecoder().decode(signerKeyBase64);
    JWSVerifier verifier = new MACVerifier(signerKeyBytes);
    SignedJWT signedJWT = SignedJWT.parse(token);
    boolean signatureValid = signedJWT.verify(verifier);

    Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
    boolean notExpired = expirationTime != null && expirationTime.after(new Date());

    if (!signatureValid) {
      throw new JwtException("Invalid JWT signature");
    }

    if (!notExpired) {
      throw new JwtValidationException(
          "JWT expired", List.of(new OAuth2Error("invalid_token", "JWT expired", null)));
    }
  }
}
