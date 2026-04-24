package com.veritas.omnigrade.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.omnigrade.common.exception.ErrorCode;
import com.veritas.omnigrade.common.response.ApiResponse;
import com.veritas.omnigrade.common.security.jwt.CustomJwtDecoder;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SecurityConfig {

  CustomJwtDecoder customJwtDecoder;
  JwtAuthenticationConverter jwtAuthenticationConverter;
  ObjectMapper objectMapper;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  // Full flow:
  // 1. Request (Authorization: Bearer <JWT>)
  //    JWT Payload: {sub (phoneNumber), user_id, session_id, exp}
  // 2. Spring Security get token
  //    --> Push Token to AuthenticationManager (main security controller)
  // 3. AuthenticationManager
  //    --> JwtAuthenticationProvider (validate, decode)
  //    --> Create Jwt object
  //    --> Push to Converter
  // 4. Converter (custom)
  //    --> Extract fields from JWT
  //    --> Push to SessionAuthorityResolver (business)
  // 5. Resolver
  //    --> Get session_id, user_id
  //    --> Redis get / DB lookup
  //    --> Resolve roles & permissions
  //    --> Push back to converter
  // 6. Converter
  //    --> Build JwtAuthenticationToken
  //    --> Push to SecurityContextHolder
  // At this point:
  // - SecurityContextHolder ALWAYS contains roles & permissions
  // - @PreAuthorize works correctly
  //
  // Summary:
  // When any request goes into system, Spring Security must know:
  // 1. Role & permission of this request
  // 2. This request's session is active or not
  //
  // To achieve this, we implement 3 core components:
  // - SessionAuthorityResolver       : resolve roles & permissions
  // - SessionGuardService            : validate session
  // - SessionAuthorityCachedService  : DB / Redis lookup
  // =========================================================

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**", "/api/v1/internal/healthz")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/ws/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, exAuth) ->
                            writeErrorResponse(
                                res,
                                req.getRequestURI(),
                                HttpStatus.UNAUTHORIZED,
                                ErrorCode.UNAUTHENTICATED))
                    .accessDeniedHandler(
                        (req, res, exDenied) ->
                            writeErrorResponse(
                                res,
                                req.getRequestURI(),
                                HttpStatus.FORBIDDEN,
                                ErrorCode.FORBIDDEN_ACTION)));

    return http.build();
  }

  private void writeErrorResponse(
      HttpServletResponse res, String path, HttpStatus status, ErrorCode errorCode)
      throws java.io.IOException {
    res.setStatus(status.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        res.getOutputStream(),
        ApiResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getDefaultMessage())
            .path(path)
            .timestamp(Instant.now())
            .build());
  }
}
