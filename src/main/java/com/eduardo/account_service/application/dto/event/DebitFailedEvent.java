package com.eduardo.account_service.application.dto.event;

import java.util.UUID;

public record DebitFailedEvent(
        UUID transferId,
        UUID sourceAccountId,
        String reason
) {}
