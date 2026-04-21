package com.eduardo.account_service.application.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountCreditedEvent(
        UUID transferId,
        UUID targetAccountId,
        BigDecimal amount
) {}
