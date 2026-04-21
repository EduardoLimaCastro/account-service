package com.eduardo.account_service.application.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestedEvent(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount
) {}
