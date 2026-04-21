package com.eduardo.account_service.infrastructure.repository.jpa;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {

    private AccountSpecification() {}

    private static final String FIELD_ACCOUNT_NUMBER = "accountNumber";
    private static final String FIELD_ACCOUNT_TYPE = "accountType";
    private static final String FIELD_ACCOUNT_STATUS = "status";

    public static Specification<AccountJpaEntity> withFilter(AccountFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.accountNumber() != null && !filter.accountNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_ACCOUNT_NUMBER)), "%" + filter.accountNumber().toLowerCase() + "%"));
            }

            if (filter.accountType() != null) {
                predicates.add(cb.equal(root.get(FIELD_ACCOUNT_TYPE), filter.accountType()));
            }

            if (filter.accountStatus() != null) {
                predicates.add(cb.equal(root.get(FIELD_ACCOUNT_STATUS), filter.accountStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
