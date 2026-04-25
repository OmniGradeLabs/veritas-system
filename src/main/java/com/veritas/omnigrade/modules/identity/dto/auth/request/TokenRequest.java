package com.veritas.omnigrade.modules.identity.dto.auth.request;

import java.util.UUID;
import lombok.Builder;

@Builder
public record TokenRequest(
    UUID userId, UUID universityId, UUID sessionId, String subject, UUID refreshJti) {}
