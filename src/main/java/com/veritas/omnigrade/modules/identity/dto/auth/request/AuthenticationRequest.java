package com.veritas.omnigrade.modules.identity.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
    @NotBlank(message = "Mã trường không được để trống") String universityCode,
    @NotBlank(message = "Tài khoản không được để trống") String username,
    @NotBlank(message = "Mật khẩu không được để trống") String password) {}
