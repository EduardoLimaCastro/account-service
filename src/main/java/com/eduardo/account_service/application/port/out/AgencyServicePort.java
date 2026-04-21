package com.eduardo.account_service.application.port.out;

import com.eduardo.account_service.application.dto.external.AgencySummary;

import java.util.UUID;

public interface AgencyServicePort {

    AgencySummary findAgencyById(UUID id);
}
