package com.eduardo.account_service.domain.exceptions;

import java.util.UUID;

public class AgencyNotFoundException extends RuntimeException {

    public AgencyNotFoundException(UUID agencyId) {
        super("Agency not found with id: " + agencyId);
    }
}
