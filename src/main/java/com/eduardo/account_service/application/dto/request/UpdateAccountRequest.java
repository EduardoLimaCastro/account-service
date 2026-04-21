package com.eduardo.account_service.application.dto.request;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateAccountRequest(
        @NotBlank  String ownerId,
        @NotBlank  String accountDigit,
        @NotBlank  String agencyId,
        @NotNull   AccountStatus status,
        @NotNull @Positive BigDecimal overdraftLimit,
        @NotNull @Positive BigDecimal transferLimit,
        @NotNull   AccountType accountType,
                   boolean fraudBlocked
) {}
