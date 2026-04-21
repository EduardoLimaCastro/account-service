package com.eduardo.account_service.application.dto.external;

import java.util.UUID;

public record AgencySummary(
        UUID id,
        String name,
        String code,
        String digit,
        String status
) {
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
