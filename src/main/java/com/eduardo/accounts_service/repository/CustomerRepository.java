package com.eduardo.accounts_service.repository;

import com.eduardo.accounts_service.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<AccountEntity, Long> {
}
