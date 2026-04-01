package com.eduardo.account_service.application.service;

import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;
import com.eduardo.account_service.application.port.in.CreateAccountUseCase;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService implements CreateAccountUseCase {

    private final AccountRepositoryPort repository;

    @Override
    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = Account.create(
                request.ownerId(),
                request.accountDigit(),
                request.agencyId(),
                request.balance(),
                request.overdraftLimit(),
                request.transferLimit(),
                request.accountType()
        );

        Account saved = repository.save(account);

        return new AccountResponse(
                saved.getId(),
                saved.getOwnerId(),
                saved.getAccountNumber(),
                saved.getAccountDigit(),
                saved.getAgencyId(),
                saved.getStatus(),
                saved.getBalance(),
                saved.getOverdraftLimit(),
                saved.getTransferLimit(),
                saved.getAccountType(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
