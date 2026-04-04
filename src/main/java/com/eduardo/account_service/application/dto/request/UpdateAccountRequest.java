package com.eduardo.account_service.application.dto.request;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateAccountRequest(
        @NotBlank String ownerId,
        @NotBlank String accountNumber,
        @NotBlank String accountDigit,
        @NotBlank String agencyId,
        @NotBlank AccountStatus status,
        @NotBlank BigDecimal balance,
        @NotBlank BigDecimal overdraftLimit,
        @NotBlank BigDecimal transferLimit,
        @NotBlank AccountType accountType,
        @NotBlank boolean fraudBlocked
) {}
