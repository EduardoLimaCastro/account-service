package com.eduardo.account_service.application.port.out;

import com.eduardo.account_service.domain.model.Account;

public interface AccountRepositoryPort {
    Account save(Account account);
}
