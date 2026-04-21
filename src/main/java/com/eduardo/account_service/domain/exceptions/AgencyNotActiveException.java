package com.eduardo.account_service.domain.exceptions;

import java.util.UUID;

public class AgencyNotActiveException extends RuntimeException {

    public AgencyNotActiveException(UUID agencyId) {
        super("Agency is not active and cannot accept new accounts: " + agencyId);
    }
}
