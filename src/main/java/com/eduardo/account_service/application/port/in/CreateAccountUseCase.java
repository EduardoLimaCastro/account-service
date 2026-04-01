package com.eduardo.account_service.application.port.in;

import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;

public interface CreateAccountUseCase {
    AccountResponse create(CreateAccountRequest request);
}
