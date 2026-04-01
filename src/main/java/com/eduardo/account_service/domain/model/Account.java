package com.eduardo.account_service.domain.model;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import com.eduardo.account_service.domain.exceptions.InvalidAccountStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Account {

    // =========================
    // Attributes
    // =========================

    private UUID id;
    private String ownerId;
    private String accountNumber;
    private String accountDigit;
    private String agencyId;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private BigDecimal transferLimit;
    private AccountType accountType;
    private boolean fraudBlocked;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================
    // Constructor
    // =========================

    private Account(
            UUID id,
            String ownerId,
            String accountNumber,
            String accountDigit,
            String agencyId,
            AccountStatus status,
            BigDecimal balance,
            BigDecimal overdraftLimit,
            BigDecimal transferLimit,
            AccountType accountType,
            boolean fraudBlocked,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.accountNumber = accountNumber;
        this.accountDigit = accountDigit;
        this.agencyId = agencyId;
        this.status = status;
        this.balance = balance;
        this.overdraftLimit = overdraftLimit;
        this.transferLimit = transferLimit;
        this.accountType = accountType;
        this.fraudBlocked = fraudBlocked;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // =========================
    // Factory Methods
    // =========================

    public static Account create(
            String ownerId,
            String accountDigit,
            String agencyId,
            BigDecimal balance,
            BigDecimal overdraftLimit,
            BigDecimal transferLimit,
            AccountType accountType
    ){
        return new Account(
                UUID.randomUUID(),
                ownerId,
                null,
                accountDigit,
                agencyId,
                AccountStatus.ACTIVE,
                balance,
                overdraftLimit,
                transferLimit,
                accountType,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static Account reconstitute(
            UUID id,
            String ownerId,
            String accountNumber,
            String accountDigit,
            String agencyId,
            AccountStatus status,
            BigDecimal balance,
            BigDecimal overdraftLimit,
            BigDecimal transferLimit,
            AccountType accountType,
            boolean fraudBlocked,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {

        return new Account(
                id,
                ownerId,
                accountNumber,
                accountDigit,
                agencyId,
                status,
                balance,
                overdraftLimit,
                transferLimit,
                accountType,
                fraudBlocked,
                version,
                createdAt,
                updatedAt
        );
    }

    // =========================
    // Domain Behavior
    // =========================

    public void activate() {
        changeStatus(AccountStatus.ACTIVE);
    }
    public void block() {
        changeStatus(AccountStatus.BLOCKED);
    }
    public void close() {
        changeStatus(AccountStatus.CLOSED);
    }
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if(status.equals(AccountStatus.ACTIVE)) {
            this.balance = this.balance.add(amount);
            touch();
        } else {
            throw new IllegalStateException("Only active accounts can receive deposits.");
        }
    }
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if(status.equals(AccountStatus.ACTIVE)) {
            BigDecimal availableFunds = this.balance.add(this.overdraftLimit);
            if (availableFunds.compareTo(amount) >= 0) {
                this.balance = this.balance.subtract(amount);
                touch();
            } else {
                throw new IllegalStateException("Insufficient funds for withdrawal.");
            }
        } else {
            throw new IllegalStateException("Only active accounts can perform withdrawals.");
        }
    }
    public void transferFunds(BigDecimal amount) {
        if(status.equals(AccountStatus.ACTIVE)) {
            if (amount.compareTo(this.transferLimit) <= 0) {
                withdraw(amount);
            } else {
                throw new IllegalStateException("Transfer amount exceeds the transfer limit.");
            }
        } else {
            throw new IllegalStateException("Only active accounts can perform transfers.");
        }
    }
    public void blockForFraud() {
        this.fraudBlocked = true;
    }
    public void unblockForFraud() {
        this.fraudBlocked = false;
    }
    public void changeStatus(AccountStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidAccountStateTransitionException(
                    this.id,
                    this.status,
                    newStatus
            );
        }

        this.status = newStatus;
        touch();
    }
    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
    // =========================
    // Getters
    // =========================

    public UUID getId() {return id;}
    public String getOwnerId() {return ownerId;}
    public String getAccountNumber() {return accountNumber;}
    public String getAccountDigit() {return accountDigit;}
    public String getAgencyId() {return agencyId;}
    public AccountStatus getStatus() {return status;}
    public BigDecimal getBalance() {return balance;}
    public BigDecimal getOverdraftLimit() {return overdraftLimit;}
    public BigDecimal getTransferLimit() {return transferLimit;}
    public AccountType getAccountType() {return accountType;}
    public boolean isFraudBlocked() {return fraudBlocked;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public LocalDateTime getUpdatedAt() {return updatedAt;}
    public Long getVersion() {return version;}
}
