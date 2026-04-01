package com.eduardo.account_service.infrastructure.repository.adapter;

import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.model.Account;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountJpaRepository;
import com.eduardo.account_service.infrastructure.repository.mapper.AccountJpaMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository accountJpaRepository) {this.jpaRepository = accountJpaRepository;}

    @Override
    public Account save(Account account) {
        return AccountJpaMapper.toDomain(jpaRepository.save(AccountJpaMapper.toEntity(account)));
    }

}
