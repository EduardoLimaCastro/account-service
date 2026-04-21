package com.eduardo.account_service.application.port.out;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {
    Account save(Account account);
    Page<Account> list(AccountFilter filter, Pageable pageable);
    Optional<Account> findById(UUID id);
    boolean existsById(UUID id);
    void deleteById(UUID id);
}
