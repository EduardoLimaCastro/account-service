package com.eduardo.account_service.application.dto.request;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;

public record AccountFilter (
        String accountNumber,
        AccountType accountType,
        AccountStatus accountStatus
) {}
