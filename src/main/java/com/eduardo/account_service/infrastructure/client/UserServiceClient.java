package com.eduardo.account_service.infrastructure.client;

import com.eduardo.account_service.application.dto.external.UserSummary;
import com.eduardo.account_service.application.port.out.UserServicePort;
import com.eduardo.account_service.domain.exceptions.OwnerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UserServiceClient implements UserServicePort {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("userRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public UserSummary findUserById(UUID id) {
        log.debug("Fetching user from user-service: {}", id);
        try {
            return restClient.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .body(UserSummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new OwnerNotFoundException(id);
        }
    }
}
