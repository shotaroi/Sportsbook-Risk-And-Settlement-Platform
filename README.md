# Sportsbook Risk & Settlement Platform

A portfolio-grade B2B sports betting platform implementing **Risk & Liability Engine** and **Settlement Service**. Target audience: sportsbook operators like Kambi.

## Tech Stack

- **Java 21** | **Spring Boot 3.2** | **Maven**
- **PostgreSQL** | **Spring Data JPA** (Hibernate) | **Flyway**
- **Spring Security**: JWT for `/api/**`, Basic Auth for `/admin/**`
- **Testcontainers** (Postgres) for integration tests
- **Micrometer + Actuator** | **OpenAPI/Swagger** (springdoc)

## Architecture

```
com.shotaroi.sportsbook
├── common/          # Errors, idempotency, money util
├── betting/         # Bet placement API, bet model
├── risk/            # Exposure, limits, decision, reservation
├── settlement/      # Result ingest, settlement engine
├── ledger/          # Immutable money movements
└── security/        # JWT, Basic Auth
```

**Package-by-feature**: each domain has its own `entity`, `repository`, `service`, `controller`, `dto`.

## Domain Concepts

### Exposure & Liability

- **Exposure** = reserved liability per `(eventId, marketType, selection)`.
- **Liability** = potential payout − stake = `stake × (odds − 1)`.
- Limits can be set at: **GLOBAL**, **EVENT**, or **EVENT_MARKET_SELECTION**.

### Settlement

- Admin posts result: `winningSelection` (HOME/DRAW/AWAY) or `null` (VOID).
- Settlement engine finds all PLACED bets, credits/refunds via Ledger, releases exposure.

### Concurrency (Optimistic Locking)

- `Exposure` uses `@Version` for optimistic locking.
- On concurrent bet placement, `OptimisticLockException` triggers retry (up to 10 attempts).
- Alternative: atomic SQL `UPDATE exposure SET liability = liability + :delta WHERE ... AND liability + :delta <= limit`.

### Idempotency

- **Bet placement**: key `(customerId, idempotencyKey)`; same key + same request hash → cached response.
- **Result ingest**: key `(eventId, idempotencyKey)`; same key + same request → no double settlement.

## Quick Start

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run tests
mvn test

# 3. Run application
mvn spring-boot:run
```

## API Examples

### 1. Get JWT token

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1}'
```

### 2. Place bet (Idempotency-Key required)

```bash
curl -X POST http://localhost:8080/api/bets \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Idempotency-Key: bet-001" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "eventId": "evt-1",
    "marketType": "MATCH_WINNER",
    "selection": "HOME",
    "odds": 1.85,
    "stake": 100
  }'
```

### 3. Get bet

```bash
curl -X GET http://localhost:8080/api/bets/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### 4. Get ledger

```bash
curl -X GET "http://localhost:8080/api/customers/1/ledger?page=0&size=20" \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Post result (Admin, Basic Auth)

```bash
curl -X POST http://localhost:8080/admin/events/evt-1/result \
  -u admin:admin-secret \
  -H "Idempotency-Key: result-001" \
  -H "Content-Type: application/json" \
  -d '{"winningSelection": "HOME"}'
```

For VOID (refund all):

```bash
curl -X POST http://localhost:8080/admin/events/evt-1/result \
  -u admin:admin-secret \
  -H "Idempotency-Key: result-void" \
  -H "Content-Type: application/json" \
  -d '{"winningSelection": null}'
```

### 6. Get exposures

```bash
curl -X GET "http://localhost:8080/admin/exposures?eventId=evt-1" \
  -u admin:admin-secret
```

### 7. Set limit

```bash
curl -X POST http://localhost:8080/admin/limits \
  -u admin:admin-secret \
  -H "Content-Type: application/json" \
  -d '{
    "scopeType": "EVENT_MARKET_SELECTION",
    "scopeId": "evt-1|MATCH_WINNER|HOME",
    "maxReservedLiability": 5000,
    "maxStakePerBet": 500
  }'
```

## Swagger UI

- **API docs**: http://localhost:8080/api-docs
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## Data Model (Flyway)

- `customers`, `wallets`
- `ledger_entries` (append-only)
- `bets`
- `exposures` (with `version` for optimistic locking)
- `limits`
- `idempotency_keys`
- `event_results`

## Configuration

| Property | Description |
|----------|-------------|
| `jwt.secret` | JWT signing key (min 32 chars) |
| `admin.username` | Admin Basic Auth user |
| `admin.password` | Use `{noop}plain` for dev, bcrypt hash for prod |
