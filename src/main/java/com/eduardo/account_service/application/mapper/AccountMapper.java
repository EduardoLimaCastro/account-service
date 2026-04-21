package com.eduardo.account_service.application.mapper;

import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;
import com.eduardo.account_service.domain.model.Account;

import java.time.Clock;

public class AccountMapper {

    private AccountMapper() {}

    public static Account toDomain(CreateAccountRequest request, Clock clock) {
        return Account.create(
                request.ownerId(),
                request.accountDigit(),
                request.agencyId(),
                request.balance(),
                request.overdraftLimit(),
                request.transferLimit(),
                request.accountType(),
                clock
        );
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerId(),
                account.getAccountNumber(),
                account.getAccountDigit(),
                account.getAgencyId(),
                account.getStatus(),
                account.getBalance(),
                account.getOverdraftLimit(),
                account.getTransferLimit(),
                account.getAccountType(),
                account.isFraudBlocked(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
