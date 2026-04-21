package com.eduardo.account_service.infrastructure.client;

import com.eduardo.account_service.application.dto.external.AgencySummary;
import com.eduardo.account_service.application.port.out.AgencyServicePort;
import com.eduardo.account_service.domain.exceptions.AgencyNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AgencyServiceClient implements AgencyServicePort {

    private static final Logger log = LoggerFactory.getLogger(AgencyServiceClient.class);

    private final RestClient restClient;

    public AgencyServiceClient(@Qualifier("agencyRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AgencySummary findAgencyById(UUID id) {
        log.debug("Fetching agency from agency-service: {}", id);
        try {
            return restClient.get()
                    .uri("/api/agencies/{id}", id)
                    .retrieve()
                    .body(AgencySummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new AgencyNotFoundException(id);
        }
    }
}
