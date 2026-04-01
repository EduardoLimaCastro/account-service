package com.eduardo.account_service.infrastructure.repository.mapper;

import com.eduardo.account_service.domain.model.Account;
import com.eduardo.account_service.infrastructure.repository.jpa.AccountJpaEntity;

public class AccountJpaMapper {

    private AccountJpaMapper() {}

    public static AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.getId(),
                account.getOwnerId(),
                account.getAccountDigit(),
                account.getAgencyId(),
                account.getStatus(),
                account.getBalance(),
                account.getOverdraftLimit(),
                account.getTransferLimit(),
                account.getAccountType(),
                account.isFraudBlocked(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public static Account toDomain(AccountJpaEntity entity) {
        return Account.reconstitute(
                entity.getId(),
                entity.getOwnerId(),
                entity.getAccountNumber(),
                entity.getAccountDigit(),
                entity.getAgencyId(),
                entity.getStatus(),
                entity.getBalance(),
                entity.getOverdraftLimit(),
                entity.getTransferLimit(),
                entity.getAccountType(),
                entity.isFraudBlocked(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
