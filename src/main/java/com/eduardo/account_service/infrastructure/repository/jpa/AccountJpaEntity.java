package com.eduardo.account_service.infrastructure.repository.jpa;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="accounts")
@Getter
public class AccountJpaEntity {
    @Id
    private UUID id;

    @Column(name="owner_id", nullable=false)
    private String ownerId;

    @Generated(event = EventType.INSERT)
    @Column(name="account_number", nullable=false, unique=true, length=5, insertable=false)
    private String accountNumber;

    @Column(name="account_digit", nullable=false, length=1)
    private String accountDigit;

    @Column(name="agency_id", nullable=false)
    private String agencyId;

    @Enumerated(EnumType.STRING)
    @Column(name="account_status", nullable=false)
    private AccountStatus status;

    @Column(nullable=false, precision=19, scale=4)
    private BigDecimal balance;

    @Column(name="overdraft_limit", nullable=false, precision=19, scale=4)
    private BigDecimal overdraftLimit;

    @Column(name="transfer_limit", nullable=false, precision=19, scale=4)
    private BigDecimal transferLimit;

    @Enumerated(EnumType.STRING)
    @Column(name="account_type", nullable=false)
    private AccountType accountType;

    @Column(name="fraud_blocked", nullable=false)
    private boolean fraudBlocked;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // =========================
    // Constructors
    // =========================

    protected AccountJpaEntity() {}

    public AccountJpaEntity(
        UUID id,
        String ownerId,
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
}
