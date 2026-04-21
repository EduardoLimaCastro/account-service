package com.eduardo.account_service.application.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDebitedEvent(
        UUID transferId,
        UUID sourceAccountId,
        BigDecimal amount
) {}
