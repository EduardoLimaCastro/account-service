package com.eduardo.account_service.application.dto.response;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerId,
        String accountNumber,
        String accountDigit,
        String agencyId,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal overdraftLimit,
        BigDecimal transferLimit,
        AccountType accountType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
