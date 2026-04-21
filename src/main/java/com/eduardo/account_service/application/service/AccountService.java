package com.eduardo.account_service.application.service;

import com.eduardo.account_service.application.dto.external.AgencySummary;
import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.request.UpdateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;
import com.eduardo.account_service.application.mapper.AccountMapper;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.application.port.out.AgencyServicePort;
import com.eduardo.account_service.application.port.out.UserServicePort;
import com.eduardo.account_service.domain.exceptions.AccountNotFoundException;
import com.eduardo.account_service.domain.exceptions.AgencyNotActiveException;
import com.eduardo.account_service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepositoryPort repository;
    private final UserServicePort userServicePort;
    private final AgencyServicePort agencyServicePort;
    private final Clock clock;

    public AccountResponse create(CreateAccountRequest request) {
        UUID ownerId = UUID.fromString(request.ownerId());
        UUID agencyId = UUID.fromString(request.agencyId());

        log.info("Validating owner {} with user-service", ownerId);
        userServicePort.findUserById(ownerId);

        log.info("Validating agency {} with agency-service", agencyId);
        AgencySummary agency = agencyServicePort.findAgencyById(agencyId);
        if (!agency.isActive()) {
            throw new AgencyNotActiveException(agencyId);
        }

        log.info("Creating account for ownerId: {}, agencyId: {}", ownerId, agencyId);
        Account account = AccountMapper.toDomain(request, clock);
        Account saved = repository.save(account);
        log.info("Account created with id: {}", saved.getId());
        return AccountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> list(AccountFilter filter, Pageable pageable) {
        log.debug("Listing accounts — filter: {}, pageable: {}", filter, pageable);
        return repository.list(filter, pageable).map(AccountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        log.debug("Finding account by id: {}", id);
        return repository.findById(id)
                .map(AccountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public AccountResponse update(UUID id, UpdateAccountRequest request) {
        log.info("Updating account id: {}", id);
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.update(
                request.ownerId(),
                request.accountDigit(),
                request.agencyId(),
                request.status(),
                request.overdraftLimit(),
                request.transferLimit(),
                request.accountType(),
                request.fraudBlocked(),
                clock
        );

        Account saved = repository.save(account);
        log.info("Account id: {} updated successfully", id);
        return AccountMapper.toResponse(saved);
    }

    public void delete(UUID id) {
        log.info("Deleting account id: {}", id);
        if (!repository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Account id: {} deleted", id);
    }

    public AccountResponse activate(UUID id) {
        log.info("Activating account id: {}", id);
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.activate(clock);
        Account saved = repository.save(account);
        log.info("Account id: {} activated", id);
        return AccountMapper.toResponse(saved);
    }

    public AccountResponse block(UUID id) {
        log.info("Blocking account id: {}", id);
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.block(clock);
        Account saved = repository.save(account);
        log.info("Account id: {} blocked", id);
        return AccountMapper.toResponse(saved);
    }

    public AccountResponse close(UUID id) {
        log.info("Closing account id: {}", id);
        Account account = repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.close(clock);
        Account saved = repository.save(account);
        log.info("Account id: {} closed", id);
        return AccountMapper.toResponse(saved);
    }
}
