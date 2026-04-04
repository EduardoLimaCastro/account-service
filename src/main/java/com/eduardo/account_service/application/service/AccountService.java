package com.eduardo.account_service.application.service;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.request.UpdateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;
import com.eduardo.account_service.application.mapper.AccountMapper;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.exceptions.AccountNotFoundException;
import com.eduardo.account_service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepositoryPort repository;
    private final Clock clock;

    public AccountResponse create(CreateAccountRequest request) {
        Account account = AccountMapper.toDomain(request, clock);
        Account saved = repository.save(account);
        return AccountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> list(AccountFilter filter, Pageable pageable) {
        return repository.list(filter, pageable).map(AccountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return repository.findById(id)
                .map(AccountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public AccountResponse update(UUID id, UpdateAccountRequest request) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.update(
                request.ownerId(),
                request.accountNumber(),
                request.accountDigit(),
                request.agencyId(),
                request.status(),
                request.balance(),
                request.overdraftLimit(),
                request.transferLimit(),
                request.accountType(),
                request.fraudBlocked()
        );

        Account saved = repository.save(account);
        return AccountMapper.toResponse(saved);
    }

    public void delete(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        repository.deleteById(id);
    }

    public AccountResponse activate(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.activate();
        return AccountMapper.toResponse(repository.save(account));
    }

    public AccountResponse block(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.block();
        return AccountMapper.toResponse(repository.save(account));
    }

    public AccountResponse close(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.close();
        return AccountMapper.toResponse(repository.save(account));
    }

}
