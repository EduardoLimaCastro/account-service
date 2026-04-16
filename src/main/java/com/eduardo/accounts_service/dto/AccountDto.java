package com.eduardo.accounts_service.dto;

public record AccountDto(
        Long accountNumber,
        String accountType,
        String branchAddress) {
}
