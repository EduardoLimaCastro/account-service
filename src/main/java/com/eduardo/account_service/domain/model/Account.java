package com.eduardo.account_service.domain.model;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import com.eduardo.account_service.domain.exceptions.InvalidAccountStateTransitionException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
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
            AccountType accountType,
            Clock clock
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
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
                now,
                now
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

    public void activate(Clock clock) {
        changeStatus(AccountStatus.ACTIVE, clock);
    }

    public void block(Clock clock) {
        changeStatus(AccountStatus.BLOCKED, clock);
    }

    public void close(Clock clock) {
        changeStatus(AccountStatus.CLOSED, clock);
    }

    public void deposit(BigDecimal amount, Clock clock) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if (fraudBlocked)
            throw new IllegalStateException("Account is blocked due to fraud.");
        if (!status.equals(AccountStatus.ACTIVE))
            throw new IllegalStateException("Only active accounts can receive deposits.");
        this.balance = this.balance.add(amount);
        touch(clock);
    }

    public void withdraw(BigDecimal amount, Clock clock) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if (fraudBlocked)
            throw new IllegalStateException("Account is blocked due to fraud.");
        if (!status.equals(AccountStatus.ACTIVE))
            throw new IllegalStateException("Only active accounts can perform withdrawals.");
        BigDecimal availableFunds = this.balance.add(this.overdraftLimit);
        if (availableFunds.compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient funds for withdrawal.");
        this.balance = this.balance.subtract(amount);
        touch(clock);
    }

    public void transferFunds(BigDecimal amount, Clock clock) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
        if (fraudBlocked)
            throw new IllegalStateException("Account is blocked due to fraud.");
        if (!status.equals(AccountStatus.ACTIVE))
            throw new IllegalStateException("Only active accounts can perform transfers.");
        if (amount.compareTo(this.transferLimit) > 0)
            throw new IllegalStateException("Transfer amount exceeds the transfer limit.");
        withdraw(amount, clock);
    }

    public void update(
            String ownerId,
            String accountDigit,
            String agencyId,
            AccountStatus newStatus,
            BigDecimal overdraftLimit,
            BigDecimal transferLimit,
            AccountType accountType,
            boolean fraudBlocked,
            Clock clock
    ) {
        this.ownerId = ownerId;
        this.accountDigit = accountDigit;
        this.agencyId = agencyId;
        this.overdraftLimit = overdraftLimit;
        this.transferLimit = transferLimit;
        this.accountType = accountType;
        this.fraudBlocked = fraudBlocked;
        if (!this.status.equals(newStatus)) {
            changeStatus(newStatus, clock); // touch() already called inside
        } else {
            touch(clock);
        }
    }

    public void blockForFraud(Clock clock) {
        this.fraudBlocked = true;
        touch(clock);
    }

    public void unblockForFraud(Clock clock) {
        this.fraudBlocked = false;
        touch(clock);
    }

    public void changeStatus(AccountStatus newStatus, Clock clock) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidAccountStateTransitionException(this.id, this.status, newStatus);
        }
        this.status = newStatus;
        touch(clock);
    }

    private void touch(Clock clock) {
        this.updatedAt = LocalDateTime.now(clock);
    }

    // =========================
    // Identity
    // =========================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // =========================
    // Getters
    // =========================

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountDigit() { return accountDigit; }
    public String getAgencyId() { return agencyId; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public BigDecimal getTransferLimit() { return transferLimit; }
    public AccountType getAccountType() { return accountType; }
    public boolean isFraudBlocked() { return fraudBlocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
