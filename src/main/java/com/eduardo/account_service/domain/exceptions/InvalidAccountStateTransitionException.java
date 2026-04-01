package com.eduardo.account_service.domain.exceptions;

import com.eduardo.account_service.domain.enums.AccountStatus;

import java.util.UUID;

public class InvalidAccountStateTransitionException extends RuntimeException {
    public InvalidAccountStateTransitionException(UUID accountId, AccountStatus accountStatusFrom, AccountStatus accountStatusTo) {
        super("Cannot transition account %s from status %s to status %s".formatted(accountId, accountStatusFrom, accountStatusTo));
    }
}
