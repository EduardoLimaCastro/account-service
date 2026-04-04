package com.eduardo.account_service.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID>, JpaSpecificationExecutor<AccountJpaEntity> {
    boolean existsByAccountNumber(String accountNumber);
}
