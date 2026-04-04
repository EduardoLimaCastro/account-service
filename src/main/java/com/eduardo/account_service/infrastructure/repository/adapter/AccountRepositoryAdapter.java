package com.eduardo.account_service.infrastructure.repository.adapter;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.model.Account;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountJpaRepository;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountSpecification;
import com.eduardo.account_service.infrastructure.repository.mapper.AccountJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;
    private final Clock clock;

    @Override
    public Account save(Account account) {
        return AccountJpaMapper.toDomain(jpaRepository.save(AccountJpaMapper.toEntity(account)), clock);
    }

    @Override
    public Page<Account> list(AccountFilter filter, Pageable pageable) {
       return jpaRepository.findAll(AccountSpecification.withFilter(filter), pageable)
               .map(e -> AccountJpaMapper.toDomain(e, clock));
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(e -> AccountJpaMapper.toDomain(e, clock));
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    public void deleteById(UUID id) { jpaRepository.deleteById(id); }
}
