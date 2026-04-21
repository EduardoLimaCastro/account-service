# Account Service — Project Overview

> **Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 15 · Kafka · OAuth2 JWT · Clean Architecture · Saga Pattern

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Platform Context](#2-platform-context)
3. [Architecture Overview](#3-architecture-overview)
4. [Package Structure](#4-package-structure)
5. [Domain Layer](#5-domain-layer)
6. [Application Layer](#6-application-layer)
7. [Infrastructure Layer](#7-infrastructure-layer)
8. [REST API](#8-rest-api)
9. [Kafka Events — Saga Pattern](#9-kafka-events--saga-pattern)
10. [Security](#10-security)
11. [Observability](#11-observability)
12. [Database Migrations](#12-database-migrations)
13. [Configuration](#13-configuration)
14. [Testing](#14-testing)
15. [Deployment](#15-deployment)
16. [Key Design Decisions](#16-key-design-decisions)

---

## 1. Purpose

The **Account Service** is a microservice responsible for all lifecycle management of financial accounts within the Fintech platform. Its core responsibilities are:

- **CRUD** operations on accounts (create, read, update, delete)
- **State machine** for account status transitions (ACTIVE → BLOCKED → CLOSED)
- **Balance operations** — deposits, withdrawals, and overdraft enforcement
- **Saga participant** — executes debit, credit, and refund commands issued by the Transfer Service during inter-account transfers
- **Idempotent event consumption** — guarantees exactly-once processing of Kafka commands

---

## 2. Platform Context

This service lives inside a multi-repo Fintech platform. Each service is independent, with its own database and message contract.

```
Fintech/
├── account_service/      → this service (port 8011 / Docker 8081)
├── agency_service/       → manages bank branches (port 8012)
├── user_service/         → manages customers (port 8013)
├── transaction_service/  → immutable transaction ledger (port 8014)
├── auth_service/         → JWT issuer + RBAC (port 8015)
├── transfer_service/     → Saga orchestrator (port 8016)
└── api_gateway/          → single entry point, X-Trace-Id (port 8080)
```

The Account Service communicates with:
- **User Service** — to validate that an owner exists before creating an account
- **Agency Service** — to validate that the agency is active before creating an account
- **Transfer Service** — via Kafka, acting as a Saga participant

---

## 3. Architecture Overview

The project follows **Clean Architecture** with a strict unidirectional dependency rule:

```
domain  ←  application  ←  infrastructure
```

Each layer only depends on the layer to its left. The domain has zero framework dependencies. Spring, JPA, and Kafka live entirely in the infrastructure layer.

The **Ports & Adapters** (Hexagonal) pattern is used at the boundary between application and infrastructure:

- **Output Ports** are interfaces defined in `application/port/out/`
- **Adapters** in `infrastructure/` implement those interfaces

This means the application layer is fully testable without any Spring context, database, or broker.

---

## 4. Package Structure

```
src/main/java/com/eduardo/account_service/
│
├── AccountServiceApplication.java          # Spring Boot entry point
│
├── domain/
│   ├── model/
│   │   └── Account.java                    # Core entity — all business rules live here
│   ├── enums/
│   │   ├── AccountStatus.java              # ACTIVE | BLOCKED | CLOSED + transition guards
│   │   └── AccountType.java                # PERSONAL | ENTERPRISE
│   └── exceptions/
│       ├── AccountNotFoundException.java
│       ├── AgencyNotFoundException.java
│       ├── AgencyNotActiveException.java
│       ├── OwnerNotFoundException.java
│       └── InvalidAccountStateTransitionException.java
│
├── application/
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateAccountRequest.java
│   │   │   ├── UpdateAccountRequest.java
│   │   │   └── AccountFilter.java
│   │   ├── response/
│   │   │   └── AccountResponse.java
│   │   └── event/                          # Kafka inbound & outbound message types
│   │       ├── TransferRequestedEvent.java
│   │       ├── CreditAccountCommand.java
│   │       ├── RefundAccountCommand.java
│   │       ├── AccountDebitedEvent.java
│   │       ├── DebitFailedEvent.java
│   │       ├── AccountCreditedEvent.java
│   │       ├── CreditFailedEvent.java
│   │       └── AccountRefundedEvent.java
│   ├── dto/external/
│   │   ├── UserSummary.java                # Lightweight DTO from User Service
│   │   └── AgencySummary.java              # Lightweight DTO from Agency Service
│   ├── mapper/
│   │   └── AccountMapper.java              # Static: Request→Domain, Domain→Response
│   ├── port/out/
│   │   ├── AccountRepositoryPort.java
│   │   ├── AccountEventPublisherPort.java
│   │   ├── AgencyServicePort.java
│   │   └── UserServicePort.java
│   └── service/
│       ├── AccountService.java             # CRUD + status transitions
│       └── AccountTransferService.java     # Saga participant logic
│
└── infrastructure/
    ├── repository/
    │   ├── jpa/
    │   │   ├── AccountJpaEntity.java       # Persistence model (no domain annotations)
    │   │   ├── AccountJpaRepository.java   # Spring Data + JpaSpecificationExecutor
    │   │   ├── AccountSpecification.java   # Dynamic filter-based Criteria queries
    │   │   ├── ProcessedEventJpaEntity.java # Idempotency tracking
    │   │   └── ProcessedEventJpaRepository.java
    │   ├── mapper/
    │   │   └── AccountJpaMapper.java       # Static: Domain↔JpaEntity
    │   └── adapter/
    │       └── AccountRepositoryAdapter.java
    ├── client/
    │   ├── AgencyServiceClient.java        # REST client → agency_service
    │   └── UserServiceClient.java          # REST client → user_service
    ├── messaging/
    │   ├── producer/
    │   │   └── AccountEventProducer.java   # Implements AccountEventPublisherPort
    │   └── consumer/
    │       └── AccountEventConsumer.java   # @KafkaListener — 3 topics
    ├── web/
    │   ├── controller/
    │   │   └── AccountController.java
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java # RFC 7807 ProblemDetail
    │   └── filter/
    │       └── RequestIdFilter.java        # X-Trace-Id propagation
    └── config/
        ├── ClockConfiguration.java         # Clock.systemDefaultZone() bean
        ├── KafkaTopicConfig.java           # Topic declarations
        ├── KafkaConsumerConfig.java        # Per-topic deserializer factories
        ├── SecurityConfig.java             # OAuth2 Resource Server + endpoint rules
        ├── OpenApiConfig.java              # Swagger/OpenAPI setup
        └── RestClientConfig.java           # OAuth2-authenticated RestClient beans
```

---

## 5. Domain Layer

The domain layer contains **pure Java** — no Spring, no JPA, no Lombok (only the domain entity itself). Everything here represents business rules that should never change regardless of framework.

### 5.1 Account Entity

`Account` is the central aggregate. It has:

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Primary identifier |
| `ownerId` | `String` | Reference to User Service |
| `accountNumber` | `String` | 5-digit number, DB-generated |
| `accountDigit` | `String` | 1-character check digit |
| `agencyId` | `String` | Reference to Agency Service |
| `status` | `AccountStatus` | Current account status |
| `balance` | `BigDecimal` | Current balance |
| `overdraftLimit` | `BigDecimal` | Maximum negative balance allowed |
| `transferLimit` | `BigDecimal` | Maximum single transfer amount |
| `accountType` | `AccountType` | PERSONAL or ENTERPRISE |
| `fraudBlocked` | `boolean` | Manual fraud flag |
| `version` | `Long` | Optimistic locking version |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `updatedAt` | `LocalDateTime` | Last modification timestamp |

**Private constructor** — callers must use factory methods:

```java
// New account — sets createdAt, updatedAt, generates UUID
Account.create(ownerId, accountDigit, agencyId, balance,
               overdraftLimit, transferLimit, accountType, clock);

// Load from DB — skips validation, no side-effects
Account.reconstitute(id, ownerId, accountNumber, accountDigit, agencyId,
                     status, balance, overdraftLimit, transferLimit,
                     accountType, fraudBlocked, version, createdAt, updatedAt);
```

`Clock` is always injected — never `LocalDateTime.now()` directly. This enables deterministic tests with a fixed clock.

**Domain behaviors:**

```java
account.deposit(amount, clock)        // validates positive, ACTIVE, not fraudBlocked
account.withdraw(amount, clock)       // validates sufficient funds (balance + overdraftLimit)
account.transferFunds(amount, clock)  // like withdraw + validates transferLimit
account.activate(clock)
account.block(clock)
account.close(clock)                  // terminal — no way back
account.blockForFraud(clock)
account.unblockForFraud(clock)
account.update(ownerId, digit, agencyId, status,
               overdraftLimit, transferLimit, accountType,
               fraudBlocked, clock)
```

Every state-changing method calls `changeStatus(newStatus, clock)` internally, which delegates to `AccountStatus.canTransitionTo()` and throws `InvalidAccountStateTransitionException` on invalid transitions.

### 5.2 AccountStatus — State Machine

```
        ┌──────────┐
        │  ACTIVE  │──────────────────────┐
        └────┬─────┘                       │
             │ block()                     │ close()
             ▼                             │
        ┌──────────┐                       │
        │ BLOCKED  │──────────────────────>│
        └──────────┘         close()       │
                                           ▼
                                       ┌────────┐
                                       │ CLOSED │  ← terminal
                                       └────────┘
```

Valid transitions:
- `ACTIVE → BLOCKED` ✓
- `ACTIVE → CLOSED` ✓
- `BLOCKED → ACTIVE` ✓
- `BLOCKED → CLOSED` ✓
- `CLOSED → anything` ✗ — always throws

### 5.3 AccountType

- `PERSONAL` — individual customer account
- `ENTERPRISE` — corporate account

### 5.4 Domain Exceptions

All extend `RuntimeException` and carry descriptive messages. Mapped to HTTP responses by `GlobalExceptionHandler`.

| Exception | HTTP | Cause |
|---|---|---|
| `AccountNotFoundException` | 404 | No account with given ID |
| `AgencyNotFoundException` | 404 | Agency Service returned 404 |
| `AgencyNotActiveException` | 409 | Agency exists but not ACTIVE |
| `OwnerNotFoundException` | 404 | User Service returned 404 |
| `InvalidAccountStateTransitionException` | 422 | Illegal state transition |

---

## 6. Application Layer

The application layer orchestrates use cases by coordinating the domain model with output ports. It never imports Spring beans or JPA — all I/O goes through port interfaces.

### 6.1 AccountService

Handles all account management operations:

```
create(CreateAccountRequest)
  ├── userServicePort.findUserById(ownerId)        → throws OwnerNotFoundException if absent
  ├── agencyServicePort.findAgencyById(agencyId)   → throws AgencyNotFoundException if absent
  ├── agencySummary.isActive()                     → throws AgencyNotActiveException if not
  ├── Account.create(...)
  └── accountRepositoryPort.save(account)

list(AccountFilter, Pageable)  → Page<AccountResponse>
findById(UUID)                 → AccountResponse
update(UUID, UpdateAccountRequest) → AccountResponse
delete(UUID)                   → void (hard delete)
activate(UUID)                 → AccountResponse
block(UUID)                    → AccountResponse
close(UUID)                    → AccountResponse
```

### 6.2 AccountTransferService

Implements the Saga participant role. Called exclusively by Kafka consumers.

**Debit flow** (triggered by `TransferRequestedEvent`):
1. Load source account
2. Call `account.transferFunds(amount, clock)` — enforces transfer limit + balance
3. Save account
4. On success → publish `AccountDebitedEvent`
5. On any failure → publish `DebitFailedEvent` with reason string

**Credit flow** (triggered by `CreditAccountCommand`):
1. Load target account
2. Call `account.deposit(amount, clock)`
3. Save account
4. On success → publish `AccountCreditedEvent`
5. On any failure → publish `CreditFailedEvent` with reason string

**Refund flow** (triggered by `RefundAccountCommand`):
1. Load source account
2. Call `account.deposit(amount, clock)` (returns funds)
3. Save account
4. On success → publish `AccountRefundedEvent`
5. On failure → **throws exception** → message goes to DLQ for manual intervention

### 6.3 AccountMapper

Static utility class — no instantiation:

```java
AccountMapper.toDomain(CreateAccountRequest request, Clock clock) → Account
AccountMapper.toResponse(Account account)                         → AccountResponse
```

### 6.4 Output Port Interfaces

```java
// Persistence
AccountRepositoryPort.save(Account)
AccountRepositoryPort.list(AccountFilter, Pageable)  → Page<Account>
AccountRepositoryPort.findById(UUID)                 → Optional<Account>
AccountRepositoryPort.existsById(UUID)               → boolean
AccountRepositoryPort.deleteById(UUID)

// Events
AccountEventPublisherPort.publishAccountDebited(AccountDebitedEvent)
AccountEventPublisherPort.publishDebitFailed(DebitFailedEvent)
AccountEventPublisherPort.publishAccountCredited(AccountCreditedEvent)
AccountEventPublisherPort.publishCreditFailed(CreditFailedEvent)
AccountEventPublisherPort.publishAccountRefunded(AccountRefundedEvent)

// External Services
AgencyServicePort.findAgencyById(UUID)  → AgencySummary
UserServicePort.findUserById(UUID)      → UserSummary
```

### 6.5 DTOs

**CreateAccountRequest** — validated with Bean Validation:
- `ownerId` — `@NotBlank`
- `accountDigit` — `@NotBlank`, `@Size(1,1)`
- `agencyId` — `@NotBlank`
- `balance` — `@NotNull`, `@Positive`
- `overdraftLimit` — `@NotNull`, `@Positive`
- `transferLimit` — `@NotNull`, `@Positive`
- `accountType` — `@NotNull`

**UpdateAccountRequest** — same fields plus `status` and `fraudBlocked`.

**AccountFilter** — all optional, used for list endpoint query params:
- `accountNumber` — partial LIKE match
- `accountType` — exact
- `accountStatus` — exact

**AccountResponse** — full projection returned to clients.

**External DTOs:**

```java
record UserSummary(UUID id, String username, String userType) {}

record AgencySummary(UUID id, String name, String code, String digit, String status) {
    public boolean isActive() { return "ACTIVE".equals(status); }
}
```

---

## 7. Infrastructure Layer

All Spring, JPA, Kafka, and HTTP concerns live here.

### 7.1 JPA Layer

**AccountJpaEntity** mirrors the `accounts` table:
- `@Version Long version` — enables optimistic locking
- `@Generated(event = EventType.INSERT) String accountNumber` — populated by DB sequence after INSERT, not by app code
- All `BigDecimal` fields use `precision=19, scale=4`

**AccountJpaRepository** extends both `JpaRepository` and `JpaSpecificationExecutor`, enabling dynamic Criteria API queries.

**AccountSpecification** builds a `Specification<AccountJpaEntity>` from an `AccountFilter`:
- `accountNumber` → `ILIKE '%value%'`
- `accountType` → exact match
- `accountStatus` → exact match
- All predicates combined with `cb.and(...)`, null-safe

**AccountJpaMapper** bridges domain ↔ JPA:
```java
AccountJpaMapper.toEntity(Account domain) → AccountJpaEntity
AccountJpaMapper.toDomain(AccountJpaEntity entity, Clock clock) → Account
```

**AccountRepositoryAdapter** implements `AccountRepositoryPort`, coordinating `AccountJpaRepository`, `AccountSpecification`, and `AccountJpaMapper`.

### 7.2 Idempotency

**ProcessedEventJpaEntity** has a composite primary key `(eventId, topic)`. Before processing any Kafka message, the consumer checks this table. After successful processing, it inserts the record. Duplicate messages are silently skipped.

```sql
CREATE TABLE processed_events (
    event_id    VARCHAR NOT NULL,
    topic       VARCHAR NOT NULL,
    processed_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (event_id, topic)
);
```

### 7.3 External Clients

Both clients are Spring `RestClient` beans with OAuth2 `client_credentials` token injection.

**AgencyServiceClient:**
- `GET ${AGENCY_SERVICE_URL:http://localhost:8012}/api/agencies/{id}`
- Maps 404 → `AgencyNotFoundException`

**UserServiceClient:**
- `GET ${USER_SERVICE_URL:http://localhost:8013}/users/{id}`
- Maps 404 → `OwnerNotFoundException`

### 7.4 Kafka Producer

`AccountEventProducer` implements `AccountEventPublisherPort`. Uses `KafkaTemplate<String, Object>` with JSON serialization. The partition key for every message is `transferId.toString()`, which guarantees ordering of all events for the same transfer.

### 7.5 Kafka Consumer

`AccountEventConsumer` has three `@KafkaListener` methods, one per inbound topic. Each listener:
1. Checks `ProcessedEventJpaRepository` — skip if already processed
2. Delegates to `AccountTransferService`
3. Marks event as processed
4. Acknowledges the Kafka offset (`Acknowledgment.acknowledge()`)

If an exception is thrown, the offset is not acknowledged and the message is eventually delivered to the DLQ.

### 7.6 Request Tracing Filter

`RequestIdFilter` (order=1) runs before everything else:
- Reads `X-Trace-Id` header from the request
- Generates a UUID if the header is absent
- Stores it in SLF4J MDC under `requestId`
- Echoes it back in the response header

This allows end-to-end trace correlation across services.

### 7.7 Global Exception Handler

`GlobalExceptionHandler` is a `@RestControllerAdvice` that converts exceptions to RFC 7807 `ProblemDetail` responses:

```json
{
  "type":     "/errors/account-not-found",
  "title":    "Account Not Found",
  "status":   404,
  "detail":   "Account with id 'abc...' not found.",
  "instance": "/api/accounts/abc...",
  "errors":   []
}
```

| Exception | Status |
|---|---|
| `MethodArgumentNotValidException` | 400 |
| `AccountNotFoundException` | 404 |
| `OwnerNotFoundException` | 404 |
| `AgencyNotFoundException` | 404 |
| `AgencyNotActiveException` | 409 |
| `InvalidAccountStateTransitionException` | 422 |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` | 422 |
| `Exception` (fallback) | 500 |

---

## 8. REST API

Base path: `/api/accounts`

| Method | Path | Role | Status | Description |
|---|---|---|---|---|
| `POST` | `/api/accounts` | ADMIN | 201 | Create new account |
| `GET` | `/api/accounts` | USER, ADMIN | 200 | List accounts (paginated + filtered) |
| `GET` | `/api/accounts/{id}` | USER, ADMIN | 200 | Get account by ID |
| `PUT` | `/api/accounts/{id}` | ADMIN | 200 | Full update |
| `DELETE` | `/api/accounts/{id}` | ADMIN | 204 | Hard delete |
| `PATCH` | `/api/accounts/{id}/activate` | ADMIN | 200 | Transition to ACTIVE |
| `PATCH` | `/api/accounts/{id}/block` | ADMIN | 200 | Transition to BLOCKED |
| `PATCH` | `/api/accounts/{id}/close` | ADMIN | 200 | Transition to CLOSED |

**List query parameters:**
- `accountNumber` (string, optional)
- `accountType` (PERSONAL | ENTERPRISE, optional)
- `accountStatus` (ACTIVE | BLOCKED | CLOSED, optional)
- Spring Data `Pageable` params: `page`, `size`, `sort`

**Example — Create account:**
```http
POST /api/accounts
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "ownerId": "550e8400-e29b-41d4-a716-446655440000",
  "accountDigit": "5",
  "agencyId": "660e8400-e29b-41d4-a716-446655440001",
  "balance": "1000.00",
  "overdraftLimit": "500.00",
  "transferLimit": "5000.00",
  "accountType": "PERSONAL"
}
```

```http
HTTP/1.1 201 Created
{
  "id": "...",
  "accountNumber": "00042",
  "accountDigit": "5",
  "status": "ACTIVE",
  "balance": 1000.00,
  ...
}
```

---

## 9. Kafka Events — Saga Pattern

The Account Service participates in a distributed Saga orchestrated by `transfer_service`.

### Topics

| Direction | Topic | Payload |
|---|---|---|
| Consumed | `transfer.requested` | `TransferRequestedEvent` |
| Consumed | `credit.account` | `CreditAccountCommand` |
| Consumed | `refund.account` | `RefundAccountCommand` |
| Published | `account.debited` | `AccountDebitedEvent` |
| Published | `debit.failed` | `DebitFailedEvent` |
| Published | `account.credited` | `AccountCreditedEvent` |
| Published | `credit.failed` | `CreditFailedEvent` |
| Published | `account.refunded` | `AccountRefundedEvent` |
| DLQ | `account.events.dlq` | Failed messages |

### Happy Path

```
transfer_service                 account_service
      │                               │
      │── transfer.requested ────────>│
      │                               ├─ debit source account
      │                               │
      │<── account.debited ───────────│
      │                               │
      │── credit.account ────────────>│
      │                               ├─ credit target account
      │                               │
      │<── account.credited ──────────│
      │                               │
   Transfer(COMPLETED)
```

### Compensation Flow

```
transfer_service                 account_service
      │                               │
      │── credit.account ────────────>│
      │                               ├─ credit fails
      │                               │
      │<── credit.failed ─────────────│
      │                               │
      │── refund.account ────────────>│
      │                               ├─ refund source
      │                               │
      │<── account.refunded ──────────│
      │                               │
   Transfer(FAILED)
```

### Event Payload Structure

```java
// Inbound
record TransferRequestedEvent(UUID transferId, UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {}
record CreditAccountCommand(UUID transferId, UUID targetAccountId, BigDecimal amount) {}
record RefundAccountCommand(UUID transferId, UUID sourceAccountId, BigDecimal amount) {}

// Outbound
record AccountDebitedEvent(UUID transferId, UUID sourceAccountId, BigDecimal amount) {}
record DebitFailedEvent(UUID transferId, UUID sourceAccountId, String reason) {}
record AccountCreditedEvent(UUID transferId, UUID targetAccountId, BigDecimal amount) {}
record CreditFailedEvent(UUID transferId, UUID targetAccountId, String reason) {}
record AccountRefundedEvent(UUID transferId, UUID sourceAccountId, BigDecimal amount) {}
```

**Partition key:** always `transferId.toString()` — ensures all events for a transfer land on the same partition.

**Topics config:** 3 partitions, replication factor 1.

---

## 10. Security

**Authentication:** OAuth2 Resource Server with JWT. Token issued by `auth_service` (port 8015).

**JWKS URI:** `${AUTH_JWK_URI:http://localhost:8015/oauth2/jwks}`

**Authority extraction:** The JWT `role` claim is mapped to a Spring Security authority with the `ROLE_` prefix.

**Authorization rules:**

| Path pattern | Roles |
|---|---|
| `/actuator/health`, `/actuator/prometheus`, `/actuator/metrics/**` | Public |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| `POST /api/accounts` | ADMIN |
| `PUT /api/accounts/**` | ADMIN |
| `PATCH /api/accounts/**` | ADMIN |
| `DELETE /api/accounts/**` | ADMIN |
| `GET /api/accounts`, `GET /api/accounts/**` | USER, ADMIN |
| Everything else | Authenticated |

**Session:** Stateless (JWT-based, no server-side sessions).

**Service-to-service calls** (RestClient → agency_service / user_service) use OAuth2 Client Credentials:
- Client ID: `account-service`
- Client secret: `${ACCOUNT_SERVICE_CLIENT_SECRET:account-service-secret}`
- Token endpoint: `${AUTH_TOKEN_URI:http://localhost:8015/oauth2/token}`

---

## 11. Observability

### Metrics

Micrometer + Prometheus registry. Scrape endpoint: `GET /actuator/prometheus`.

### Distributed Tracing

OpenTelemetry bridge → Zipkin exporter.
- Zipkin endpoint: `${ZIPKIN_URL:http://localhost:9411}/api/v2/spans`
- Sampling probability: 100% (all requests traced)

Trace/span IDs are injected into MDC and appear in every log line.

### Logging

Logback with two profiles:

**dev** — colored console output:
```
2024-01-15 10:30:00.123 INFO  [abc123] [span456] c.e.a.s.AccountService - Account created: ...
```

**prod** — JSON structured logs (Logstash format) shipped to Loki:
```json
{
  "timestamp": "...",
  "level": "INFO",
  "requestId": "abc123",
  "spanId": "span456",
  "logger": "com.eduardo.account_service.service.AccountService",
  "message": "Account created: ..."
}
```

Loki endpoint: `${LOKI_URL:http://localhost:3100}`

### Health & Actuator

- `/actuator/health` — liveness/readiness
- `/actuator/metrics/**` — Micrometer metrics
- `/actuator/prometheus` — Prometheus scrape

---

## 12. Database Migrations

Managed by Flyway. Scripts live in `src/main/resources/db/migration/`.

### V1 — accounts table

```sql
CREATE SEQUENCE account_number_seq START 1 MAXVALUE 99999;

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    owner_id        VARCHAR NOT NULL,
    account_number  VARCHAR(5) UNIQUE DEFAULT LPAD(nextval('account_number_seq')::TEXT, 5, '0'),
    account_digit   VARCHAR(1) NOT NULL,
    agency_id       VARCHAR NOT NULL,
    account_status  VARCHAR NOT NULL,
    balance         NUMERIC(19,4) NOT NULL,
    overdraft_limit NUMERIC(19,4) NOT NULL,
    transfer_limit  NUMERIC(19,4) NOT NULL,
    account_type    VARCHAR NOT NULL,
    fraud_blocked   BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

`account_number` is auto-generated by the sequence on INSERT. The JPA entity marks the field `insertable=false` with `@Generated(event = EventType.INSERT)` so Hibernate refreshes it after the INSERT without trying to write a value.

### V2 — processed_events table

```sql
CREATE TABLE processed_events (
    event_id     VARCHAR NOT NULL,
    topic        VARCHAR NOT NULL,
    processed_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (event_id, topic)
);
```

Used for Kafka idempotency.

---

## 13. Configuration

### application.properties (production defaults)

```properties
server.port=8011
spring.application.name=account-service

# Database (overridable via env vars)
spring.datasource.url=jdbc:postgresql://localhost:5433/account_service
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=none

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true

# OAuth2
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${AUTH_JWK_URI:http://localhost:8015/oauth2/jwks}
spring.security.oauth2.client.registration.account-service.client-id=account-service
spring.security.oauth2.client.registration.account-service.client-secret=${ACCOUNT_SERVICE_CLIENT_SECRET:account-service-secret}
spring.security.oauth2.client.registration.account-service.authorization-grant-type=client_credentials
spring.security.oauth2.client.provider.auth-server.token-uri=${AUTH_TOKEN_URI:http://localhost:8015/oauth2/token}

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=account-service-group
spring.kafka.consumer.auto-offset-reset=earliest

# Observability
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=${ZIPKIN_URL:http://localhost:9411}/api/v2/spans
```

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `AUTH_JWK_URI` | `http://localhost:8015/oauth2/jwks` | JWT public keys |
| `AUTH_TOKEN_URI` | `http://localhost:8015/oauth2/token` | Token endpoint |
| `ACCOUNT_SERVICE_CLIENT_SECRET` | `account-service-secret` | OAuth2 client secret |
| `AGENCY_SERVICE_URL` | `http://localhost:8012` | Agency Service base URL |
| `USER_SERVICE_URL` | `http://localhost:8013` | User Service base URL |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `ZIPKIN_URL` | `http://localhost:9411` | Zipkin endpoint |
| `LOKI_URL` | `http://localhost:3100` | Loki endpoint |

---

## 14. Testing

### Unit Tests — Domain

`AccountTest` covers the full domain model in isolation (no Spring context):
- Account creation and initial state assertions
- All valid and invalid status transitions
- `deposit()` — happy path, negative amount, closed account, fraud-blocked account
- `withdraw()` — happy path, insufficient funds (with/without overdraft)
- `transferFunds()` — transfer limit enforcement, balance validation
- Fraud block/unblock mechanics
- Entity equality (UUID-based)

All tests use a fixed `Clock` for deterministic timestamp assertions.

### Integration Tests

`AccountServiceApplicationTests` starts the full Spring context with Testcontainers:
- A real PostgreSQL container is spun up via `@ServiceConnection`
- Flyway migrations run against it
- No Docker configuration needed in test code
- Kafka auto-startup disabled (`spring.kafka.listener.auto-startup=false`)

### Running Tests

```bash
# All tests (Docker must be running for Testcontainers)
./mvnw test

# Single class
./mvnw test -Dtest=AccountTest

# Single method
./mvnw test -Dtest=AccountTest#deposit_shouldIncreaseBalance
```

---

## 15. Deployment

### Local Development

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Run service (activates dev profile automatically)
./mvnw spring-boot:run
# → available at http://localhost:8011
# → Swagger UI at http://localhost:8011/swagger-ui.html
```

### Full Docker Compose

```bash
docker-compose up -d
# → service at http://localhost:8081
# → PostgreSQL at localhost:5433
```

### Full Platform (from Fintech/ root)

```bash
# Start Kafka
docker-compose -f docker-compose.kafka.yml up -d

# Start observability stack
docker-compose -f docker-compose.observability.yml up -d

# Start each service
cd account_service && docker-compose up -d
cd ../agency_service && docker-compose up -d
# ... etc
```

### Build

```bash
./mvnw clean install    # compile + test + package
./mvnw clean package -DskipTests  # skip tests
```

---

## 16. Key Design Decisions

### Why private constructors + factory methods?

Forces all creation paths through named, validated methods. `create()` always sets timestamps and generates an ID. `reconstitute()` never runs validation (data already validated on write). This prevents accidental creation of half-initialized entities.

### Why is `Clock` injected?

Static `LocalDateTime.now()` makes tests non-deterministic. Injecting a `Clock` bean allows tests to pass a `Clock.fixed(...)` and assert exact timestamps. `ClockConfiguration` provides `Clock.systemDefaultZone()` in production.

### Why separate `AccountJpaEntity` and `Account`?

JPA annotations (`@Entity`, `@Column`, `@GeneratedValue`, etc.) are infrastructure concerns. Putting them on the domain model would make the domain depend on the persistence framework. Changes to the DB schema would ripple into domain code. The JPA entity is a pure persistence detail.

### Why `AccountSpecification`?

The list endpoint accepts optional filters. Building conditional `WHERE` clauses with string concatenation is fragile and SQL-injection-prone. The Criteria API with `Specification` composes null-safe predicates type-safely and plugs directly into Spring Data's `JpaSpecificationExecutor`.

### Why DB-generated account numbers?

Using a PostgreSQL sequence guarantees uniqueness under concurrent inserts without application-level locking. The app doesn't need to read the current max, increment it, and write — the DB handles that atomically. The `@Generated` annotation tells Hibernate to refresh the entity after INSERT.

### Why `ProcessedEventJpaEntity` for idempotency?

Kafka delivers messages "at least once". Without deduplication, a redelivered `transfer.requested` would debit the source account twice. The `processed_events` table with `(event_id, topic)` as PK prevents this. The `@Transactional` on each consumer handler ensures the event processing and the idempotency marker are committed atomically.

### Why `transferId` as Kafka partition key?

All events for the same transfer (debit, credit, refund) share a key, so they land on the same partition. Kafka guarantees ordering within a partition. The orchestrator receives events in the correct sequence without extra coordination.

### Why RFC 7807 ProblemDetail?

Standardized error format clients can parse programmatically. Includes `type` URI for machine-readable classification, `title` for humans, `status` for HTTP, `detail` for context-specific explanation, and `instance` for the exact request path. All services in the platform use the same format, making API integration consistent.
