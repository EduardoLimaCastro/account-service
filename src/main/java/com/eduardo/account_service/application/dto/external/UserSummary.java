package com.eduardo.account_service.application.dto.external;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String username,
        String userType
) {}
