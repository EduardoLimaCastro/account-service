package com.eduardo.account_service.application.port.out;

import com.eduardo.account_service.application.dto.external.UserSummary;

import java.util.UUID;

public interface UserServicePort {

    UserSummary findUserById(UUID id);
}
