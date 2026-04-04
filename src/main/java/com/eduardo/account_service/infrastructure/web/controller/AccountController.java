package com.eduardo.account_service.infrastructure.web.controller;

import com.eduardo.account_service.application.dto.request.AccountFilter;
import com.eduardo.account_service.application.dto.request.CreateAccountRequest;
import com.eduardo.account_service.application.dto.request.UpdateAccountRequest;
import com.eduardo.account_service.application.dto.response.AccountResponse;
import com.eduardo.account_service.application.service.AccountService;
import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> list(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false)AccountStatus accountStatus,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable){
        return ResponseEntity.ok(service.list(new AccountFilter(accountNumber, accountType, accountStatus), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<AccountResponse> block(@PathVariable UUID id) {
        return ResponseEntity.ok(service.block(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<AccountResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(service.close(id));
    }

}
