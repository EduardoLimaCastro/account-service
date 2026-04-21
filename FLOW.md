# Account Service — Fluxo Completo

## Índice

1. [Visão Geral](#visão-geral)
2. [Fluxo CRUD de Contas](#fluxo-crud-de-contas)
   - [Criação](#criação-post-apiaccounts)
   - [Busca](#busca)
   - [Atualização](#atualização-put-apiacountsid)
   - [Deleção](#deleção-delete-apiaccountsid)
   - [Mudança de Status](#mudança-de-status)
3. [Comunicação com Outros Microserviços](#comunicação-com-outros-microserviços)
4. [Kafka — Saga de Transferências](#kafka--saga-de-transferências)
5. [Observabilidade — Prometheus e Grafana](#observabilidade--prometheus-e-grafana)

---

## Visão Geral

O `account_service` é responsável pela gestão de contas financeiras dentro da plataforma Fintech. Ele segue **Clean Architecture** com camadas `domain → application → infrastructure` e expõe uma API REST na porta `8011`.

```
Client / API Gateway
       │
       ▼
  AccountController          (infrastructure/web)
       │
       ▼
  AccountService             (application/service)
       │
  ┌────┴────────────────────────────────┐
  │                                     │
  ▼                                     ▼
AccountRepositoryPort        UserServicePort / AgencyServicePort
(persistence)                (clientes HTTP para outros serviços)
```

---

## Fluxo CRUD de Contas

### Criação — `POST /api/accounts`

```
Client
  │  POST /api/accounts  { ownerId, agencyId, accountType, ... }
  ▼
AccountController.create()
  │
  ▼
AccountService.create()
  ├─► UserServicePort.findUserById(ownerId)
  │       └─► GET http://user-service:8013/users/{id}
  │           • 404 → lança OwnerNotFoundException → HTTP 404
  │
  ├─► AgencyServicePort.findAgencyById(agencyId)
  │       └─► GET http://agency-service:8012/api/agencies/{id}
  │           • 404 → lança AgencyNotFoundException → HTTP 404
  │           • agency.status != ACTIVE → lança AgencyNotActiveException → HTTP 409
  │
  ├─► Account.create(request, clock)   ← fábrica do domínio, status inicial = ACTIVE
  │
  └─► AccountRepositoryPort.save(account)
          └─► AccountRepositoryAdapter → AccountJpaRepository.save()
              • account_number gerado por sequence PostgreSQL (account_number_seq)
              • após INSERT, entidade é refrescada do DB para popular account_number

Response: AccountResponse (HTTP 201)
```

**Regras de domínio aplicadas no `Account.create()`:**
- `balance` inicial = 0
- `status` = ACTIVE
- `createdAt` / `updatedAt` definidos via `Clock` injetado

---

### Busca

#### `GET /api/accounts` — Listagem paginada

```
Client
  │  GET /api/accounts?accountType=PERSONAL&accountStatus=ACTIVE&page=0&size=20
  ▼
AccountController.list()
  │
  ▼
AccountService.list(AccountFilter, Pageable)
  │
  └─► AccountRepositoryPort.list(filter, pageable)
          └─► AccountJpaRepository.findAll(Specification, Pageable)
              • AccountSpecification monta predicados JPA dinamicamente:
                  - accountNumber: LIKE %valor% (case-insensitive)
                  - accountType:   igualdade exata
                  - accountStatus: igualdade exata
              • Parâmetros não informados são ignorados (predicado omitido)

Response: Page<AccountResponse>
```

#### `GET /api/accounts/{id}` — Busca por ID

```
AccountService.findById(UUID)
  └─► AccountRepositoryPort.findById(id)
      • presente → AccountMapper.toResponse(account)
      • ausente  → lança AccountNotFoundException → HTTP 404
```

---

### Atualização — `PUT /api/accounts/{id}`

```
Client
  │  PUT /api/accounts/{id}  { status, overdraftLimit, transferLimit, fraudBlocked, ... }
  ▼
AccountController.update()
  │
  ▼
AccountService.update(id, UpdateAccountRequest)
  ├─► AccountRepositoryPort.findById(id)   → AccountNotFoundException se não encontrado
  ├─► account.update(request, clock)       ← método do domínio
  │       • atualiza: accountType, overdraftLimit, transferLimit, fraudBlocked
  │       • se status mudou: chama changeStatus() que valida a transição via enum
  │           ACTIVE  → BLOCKED | CLOSED  ✓
  │           BLOCKED → ACTIVE  | CLOSED  ✓
  │           CLOSED  → qualquer          ✗ → InvalidAccountStateTransitionException → HTTP 422
  │       • updatedAt = clock.now()
  └─► AccountRepositoryPort.save(account)

Response: AccountResponse (HTTP 200)
```

---

### Deleção — `DELETE /api/accounts/{id}`

```
AccountService.delete(UUID)
  ├─► AccountRepositoryPort.existsById(id)  → AccountNotFoundException se false
  └─► AccountRepositoryPort.deleteById(id)

Response: HTTP 204 No Content
```

> **Nota:** deleção é física (hard delete). Considere usar `close()` para preservar histórico.

---

### Mudança de Status

Endpoints dedicados para transições de estado:

| Endpoint                          | Transição               | HTTP |
|-----------------------------------|-------------------------|------|
| `PATCH /api/accounts/{id}/activate` | → ACTIVE              | 200  |
| `PATCH /api/accounts/{id}/block`    | → BLOCKED             | 200  |
| `PATCH /api/accounts/{id}/close`    | → CLOSED (terminal)   | 200  |

Todas as transições passam por `AccountStatus.canTransitionTo()`:

```java
// Exemplo: CLOSED não permite transições
CLOSED → qualquer → InvalidAccountStateTransitionException
```

---

## Comunicação com Outros Microserviços

A comunicação HTTP entre serviços usa **Spring RestClient** com **OAuth2 Client Credentials** — cada requisição inclui automaticamente um Bearer Token obtido do `auth_service`.

```
account_service
    │
    ├─► user_service    (porta 8013)
    │       Interface: UserServicePort
    │       Impl:      UserServiceClient
    │       Endpoint:  GET /users/{id}
    │       Retorna:   UserSummary { id, username, userType }
    │       Erro:      404 → OwnerNotFoundException
    │
    └─► agency_service  (porta 8012)
            Interface: AgencyServicePort
            Impl:      AgencyServiceClient
            Endpoint:  GET /api/agencies/{id}
            Retorna:   AgencySummary { id, name, code, digit, status }
            Erros:     404 → AgencyNotFoundException
                       status != ACTIVE → AgencyNotActiveException
```

### Autenticação Inter-Serviços (OAuth2 Client Credentials)

```
AccountService (client)
    │
    │  1. Antes da chamada HTTP:
    │     POST http://auth-service:8015/oauth2/token
    │     { grant_type=client_credentials, scope=service }
    │     ← Bearer token (JWT)
    │
    │  2. Chama user/agency service com header:
    │     Authorization: Bearer <token>
    │
    └─► user_service / agency_service validam o JWT via jwk-set-uri
```

**Configuração:**
- `client-id`: `account-service`
- `scope`: `service`
- `token-uri`: `${AUTH_TOKEN_URI:http://localhost:8015/oauth2/token}`
- URLs configuráveis via env vars: `USER_SERVICE_URL`, `AGENCY_SERVICE_URL`

---

## Kafka — Saga de Transferências

O `account_service` participa como **participante** na Saga orquestrada pelo `transfer_service`. Ele consome comandos e publica eventos de resultado.

### Diagrama do Fluxo Happy Path

```
transfer_service                    account_service
      │                                   │
      │── transfer.requested ────────────►│
      │                                   │ debitSourceAccount()
      │                                   │   account.transferFunds(amount)
      │◄─ account.debited ────────────────│
      │                                   │
      │── credit.account ────────────────►│
      │                                   │ creditTargetAccount()
      │                                   │   account.deposit(amount)
      │◄─ account.credited ───────────────│
      │                                   │
      │       Transfer(COMPLETED)         │
```

### Diagrama do Fluxo de Compensação

```
transfer_service                    account_service
      │                                   │
      │── transfer.requested ────────────►│
      │                                   │ debitSourceAccount() FALHA
      │◄─ debit.failed ───────────────────│
      │   Transfer(FAILED)                │
      │
      │── credit.account ────────────────►│
      │                                   │ creditTargetAccount() FALHA
      │◄─ credit.failed ──────────────────│
      │   Transfer(COMPENSATING)          │
      │                                   │
      │── refund.account ────────────────►│
      │                                   │ refundSourceAccount()
      │                                   │   account.deposit(amount)
      │◄─ account.refunded ───────────────│
      │   Transfer(FAILED)                │
```

### Topics Kafka

| Topic               | Direção   | Produzido por      | Consumido por      |
|---------------------|-----------|--------------------|--------------------|
| `transfer.requested`| →entrada  | transfer_service   | account_service    |
| `credit.account`    | →entrada  | transfer_service   | account_service    |
| `refund.account`    | →entrada  | transfer_service   | account_service    |
| `account.debited`   | ←saída    | account_service    | transfer_service   |
| `debit.failed`      | ←saída    | account_service    | transfer_service   |
| `account.credited`  | ←saída    | account_service    | transfer_service   |
| `credit.failed`     | ←saída    | account_service    | transfer_service   |
| `account.refunded`  | ←saída    | account_service    | transfer_service   |
| `account.events.dlq`| ←saída    | account_service    | monitoramento      |

Todos os topics são criados com **3 partições** e **1 réplica** via beans `NewTopic` no `KafkaTopicConfig`.

### Idempotência (Deduplicação)

Para evitar reprocessamento de mensagens duplicadas (at-least-once delivery do Kafka):

```
AccountEventConsumer.onTransferRequested(event, ack)
  │
  ├─► processedEventRepository.existsByEventIdAndTopic(eventId, topic)
  │       TRUE  → log WARN + ack.acknowledge() + return   (descarta duplicata)
  │       FALSE → processa normalmente
  │
  ├─► accountTransferService.debitSourceAccount(event)
  │
  ├─► processedEventRepository.save(ProcessedEvent{eventId, topic})
  │
  └─► ack.acknowledge()   ← commit manual do offset
```

A tabela `processed_events (event_id, topic)` persiste os IDs já processados.

### Chave Kafka

A chave de todas as mensagens é o `transferId` (UUID). Isso garante que eventos da mesma transferência sempre vão para a mesma partição, preservando a ordem causal.

### Consumer Group & ACK Mode

- **Group ID:** `account-service-group`
- **ACK Mode:** `MANUAL` (`enable-auto-commit=false`)
- O offset só é commitado após processamento bem-sucedido + persistência do evento

---

## Observabilidade — Prometheus e Grafana

### Visão Geral do Pipeline

```
account_service (JVM)
    │
    │  Micrometer coleta métricas internas:
    │  • Contadores HTTP (requests, status codes)
    │  • Latência de endpoints (histogramas)
    │  • Pool de conexões JPA/HikariCP
    │  • Métricas JVM (heap, GC, threads)
    │  • Kafka consumer lag
    │
    ▼
/actuator/prometheus   ← endpoint HTTP exposto pela aplicação
    │
    │  Prometheus faz scrape a cada N segundos (pull model)
    ▼
Prometheus (armazena séries temporais)
    │
    ▼
Grafana (visualização e alertas)
```

### Coleta de Métricas (Micrometer → Prometheus)

O `spring-boot-starter-actuator` + `micrometer-registry-prometheus` fazem o trabalho automaticamente:

1. **Micrometer** instrumenta a aplicação internamente (intercepta chamadas HTTP, JPA, etc.)
2. O endpoint `/actuator/prometheus` serializa as métricas no formato Prometheus (text/plain)
3. **Prometheus** raspa esse endpoint periodicamente (`scrape_interval`)
4. Cada métrica recebe a tag `application=account-service` (configurada em `management.metrics.tags.application`)

**Endpoints Actuator expostos:**

| Endpoint                   | Propósito                              |
|----------------------------|----------------------------------------|
| `/actuator/health`         | Status da aplicação (DB, Kafka, disco) |
| `/actuator/info`           | Metadados do serviço                   |
| `/actuator/metrics`        | Métricas individuais (JSON)            |
| `/actuator/prometheus`     | Todas as métricas no formato Prometheus|

### Distributed Tracing (Zipkin)

```
account_service
    │  Cada request recebe um traceId + spanId (W3C TraceContext)
    │
    ├─► RequestIdFilter propaga X-Trace-Id no MDC (logs correlacionados)
    │
    └─► opentelemetry-exporter-zipkin envia spans para:
            ${ZIPKIN_URL:http://localhost:9411}/api/v2/spans

Sampling: 100% (management.tracing.sampling.probability=1.0)
```

O `micrometer-tracing-bridge-otel` faz a ponte entre a API Micrometer e o OpenTelemetry SDK.

### Logs Estruturados (Loki)

```
account_service (Logback)
    │
    │  logstash-logback-encoder → formato JSON estruturado
    │  loki-logback-appender    → envia logs diretamente ao Loki
    │
    └─► Loki (${LOKI_URL:http://localhost:3100})
            │
            ▼
        Grafana (datasource Loki — consulta logs por label/traceId)
```

Cada log JSON inclui automaticamente:
- `traceId` / `spanId` (correlação com Zipkin)
- `requestId` (X-Trace-Id via MDC)
- `level`, `logger`, `message`, `timestamp`
- `application=account-service`

### Configuração Resumida

```properties
# Endpoints expostos ao Prometheus
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Tag global aplicada a todas as métricas
management.metrics.tags.application=${spring.application.name}

# Tracing — 100% das requests amostradas
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=${ZIPKIN_URL:http://localhost:9411}/api/v2/spans

# Loki (logs)
loki.url=${LOKI_URL:http://localhost:3100}
```

### Stack de Observabilidade Completa

```
┌──────────────────────────────────────────────────┐
│              account_service                      │
│  Micrometer → /actuator/prometheus                │
│  OTel SDK   → Zipkin spans                        │
│  Logback    → Loki (JSON logs)                    │
└──────────────┬───────────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────────┐
    ▼          ▼          ▼              ▼
Prometheus   Zipkin      Loki         (futura extensão)
(métricas)  (traces)   (logs)
    │          │          │
    └──────────┴──────────┘
               │
            Grafana
    (dashboards + alertas unificados)
```

No Grafana é possível correlacionar os três sinais: ao abrir um trace no Zipkin embedded, os logs da mesma `traceId` são buscados no Loki, e as métricas do mesmo intervalo são consultadas no Prometheus — tudo na mesma janela de tempo.
