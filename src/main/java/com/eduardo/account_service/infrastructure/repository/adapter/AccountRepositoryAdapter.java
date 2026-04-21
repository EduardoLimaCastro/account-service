package com.eduardo.account_service.infrastructure.repository.adapter;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.model.Account;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountJpaRepository;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountSpecification;
import com.eduardo.account_service.infrastructure.repository.mapper.AccountJpaMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(AccountRepositoryAdapter.class);

    private final AccountJpaRepository jpaRepository;

    @Override
    public Account save(Account account) {
        log.debug("Persisting account id: {}", account.getId());
        Account saved = AccountJpaMapper.toDomain(jpaRepository.save(AccountJpaMapper.toEntity(account)));
        log.debug("Account id: {} persisted", saved.getId());
        return saved;
    }

    @Override
    public Page<Account> list(AccountFilter filter, Pageable pageable) {
        log.debug("Querying accounts — filter: {}, pageable: {}", filter, pageable);
        return jpaRepository.findAll(AccountSpecification.withFilter(filter), pageable)
                .map(AccountJpaMapper::toDomain);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        log.debug("Querying account by id: {}", id);
        return jpaRepository.findById(id).map(AccountJpaMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        log.debug("Checking existence of account id: {}", id);
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Deleting account id: {} from database", id);
        jpaRepository.deleteById(id);
    }
}
