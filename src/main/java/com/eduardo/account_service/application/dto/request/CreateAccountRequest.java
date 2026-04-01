package com.eduardo.account_service.application.dto.request;

import com.eduardo.account_service.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String ownerId,
        @NotBlank @Size(min = 1, max = 1) String accountDigit,
        @NotBlank String agencyId,
        @NotNull  @Positive BigDecimal balance,
        @NotNull  @Positive BigDecimal overdraftLimit,
        @NotNull  @Positive BigDecimal transferLimit,
        @NotNull  AccountType accountType
) {}
