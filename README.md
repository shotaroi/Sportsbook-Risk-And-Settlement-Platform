# Sportsbook Risk & Settlement Platform

A **portfolio-grade B2B sports betting platform** implementing a **Risk & Liability Engine** and **Settlement Service**. Built for sportsbook operators (e.g. Kambi, Betsson) who need to manage exposure, enforce limits, and settle bets correctly under concurrency.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Core Flows](#core-flows)
- [Domain Concepts](#domain-concepts)
- [Technical Decisions](#technical-decisions)
- [Data Model](#data-model)
- [API Reference](#api-reference)
- [Quick Start](#quick-start)
- [Tech Stack](#tech-stack)

---

## Overview

This platform handles two critical capabilities for a sportsbook:

1. **Risk & Liability Engine** — Decides whether to ACCEPT, ACCEPT_WITH_LIMIT, or REJECT each bet based on exposure limits. Reserves liability atomically under concurrent load.
2. **Settlement Service** — Ingests event results (HOME/DRAW/AWAY or VOID), settles all affected bets, credits payouts or refunds via an immutable ledger, and releases reserved exposure.

**MVP scope**: Single bets only (one selection per bet), MATCH_WINNER market, decimal odds, SEK currency.

---

## Architecture

### High-Level System Flow

```mermaid
flowchart TB
    subgraph Client["Client Layer"]
        API["Public API (JWT)"]
        Admin["Admin API (Basic Auth)"]
    end

    subgraph App["Application Layer"]
        Betting["Betting Module"]
        Risk["Risk Module"]
        Settlement["Settlement Module"]
        Ledger["Ledger Module"]
    end

    subgraph Data["Data Layer"]
        DB[(PostgreSQL)]
    end

    API --> Betting
    API --> Ledger
    Admin --> Risk
    Admin --> Settlement

    Betting --> Risk
    Betting --> Ledger
    Settlement --> Ledger
    Settlement --> Risk

    Betting --> DB
    Risk --> DB
    Settlement --> DB
    Ledger --> DB
```

### Package Structure (Package-by-Feature)

```mermaid
flowchart LR
    subgraph common["common/"]
        C1[errors]
        C2[idempotency]
        C3[money util]
    end

    subgraph betting["betting/"]
        B1[controller]
        B2[service]
        B3[entity]
        B4[repository]
    end

    subgraph risk["risk/"]
        R1[controller]
        R2[engine]
        R3[reservation]
        R4[entity]
    end

    subgraph settlement["settlement/"]
        S1[controller]
        S2[service]
        S3[entity]
    end

    subgraph ledger["ledger/"]
        L1[controller]
        L2[service]
        L3[entity]
    end

    subgraph security["security/"]
        SEC1[JWT]
        SEC2[Basic Auth]
    end

    betting --> risk
    betting --> ledger
    settlement --> ledger
    settlement --> risk
```

---

## Core Flows

### Bet Placement Flow

```mermaid
flowchart TD
    A[Client: POST /api/bets] --> B{Idempotency Key<br/>exists?}
    B -->|Yes, same request| C[Return cached response]
    B -->|No or different request| D[Validate customer exists]
    D --> E[Validate odds & stake]
    E --> F[Risk Engine: evaluate limits]
    F --> G{Decision?}
    G -->|REJECT| H[Return REJECT response]
    G -->|ACCEPT / ACCEPT_WITH_LIMIT| I[Reserve exposure<br/>optimistic lock + retry]
    I --> J[Persist bet PLACED]
    J --> K[Debit stake from Ledger]
    K --> L[Store idempotency key]
    L --> M[Return bet response]
```

### Settlement Flow

```mermaid
flowchart TD
    A[Admin: POST /admin/events/:id/result] --> B{Idempotency Key<br/>exists?}
    B -->|Yes, same request| C[Return cached response]
    B -->|No| D{Event result<br/>already recorded?}
    D -->|Yes| E[Skip - idempotent]
    D -->|No| F[Record event result]
    F --> G[Find all PLACED bets for event]
    G --> H[For each bet]
    H --> I{Result type?}
    I -->|WON| J[Credit payout to Ledger]
    I -->|LOST| K[No credit]
    I -->|VOID| L[Refund stake to Ledger]
    J --> M[Release exposure]
    K --> M
    L --> M
    M --> N[Mark bet SETTLED_*]
    N --> O[Store idempotency key]
```

### Risk Decision Flow

```mermaid
flowchart TD
    A[Evaluate bet request] --> B[Check max stake per bet limit]
    B --> C{Stake > max?}
    C -->|Yes| D[ACCEPT_WITH_LIMIT<br/>maxAllowedStake]
    C -->|No| E[Get current reserved liability]
    E --> F[Get max liability limit]
    F --> G{New liability > limit?}
    G -->|Yes| H[ACCEPT_WITH_LIMIT or REJECT]
    G -->|No| I[ACCEPT]
```

### Ledger Balance Derivation

```mermaid
flowchart LR
    subgraph Entries["Ledger Entries (append-only)"]
        E1[DEBIT: -100]
        E2[CREDIT: +200]
        E3[REFUND: +50]
    end

    Entries --> Sum["SUM(amount) by type"]
    Sum --> Balance["Balance = Credits - Debits"]
```

---

## Domain Concepts

### Exposure & Liability

| Term | Definition |
|------|------------|
| **Exposure** | Per `(eventId, marketType, selection)` — tracks how much we could pay out if that selection wins |
| **Liability** | `potentialPayout - stake` = `stake × (odds - 1)` |
| **Reserved liability** | Sum of liabilities from all PLACED bets on that selection |

### Limit Scopes

```mermaid
flowchart TD
    subgraph Limits["Limit Hierarchy"]
        GLOBAL["GLOBAL<br/>Applies to all"]
        EVENT["EVENT<br/>Per eventId"]
        EMS["EVENT_MARKET_SELECTION<br/>Per eventId|marketType|selection"]
    end

    GLOBAL --> EVENT
    EVENT --> EMS
```

### Bet Status Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PLACED: Bet accepted
    PLACED --> SETTLED_WON: Result = selection wins
    PLACED --> SETTLED_LOST: Result = other selection wins
    PLACED --> SETTLED_VOID: Result = VOID
```

---

## Technical Decisions

### Concurrency: Optimistic Locking + Retry

```mermaid
flowchart TD
    A[Place bet] --> B[Load Exposure with @Version]
    B --> C[Add liability]
    C --> D[Save Exposure]
    D --> E{OptimisticLockException?}
    E -->|Yes| F[Retry up to 10x]
    F --> B
    E -->|No| G[Success]
```

- **Why**: JPA-friendly, avoids long-held locks, works well with connection pooling
- **Alternative**: Atomic SQL `UPDATE exposure SET liability = liability + :delta WHERE ... AND liability + :delta <= limit` (mentioned in README for future improvement)

### Idempotency Strategy

| Scope | Key | Use Case |
|-------|-----|----------|
| Bet placement | `(customerId, idempotencyKey)` | Same key + same request hash → return cached bet response |
| Result ingest | `(eventId, idempotencyKey)` | Same key + same request → no double settlement |

Different request body with same key → `409 Conflict` (DuplicateIdempotencyKeyException).

### Money Handling

- **BigDecimal** for all money and odds (no `float`/`double`)
- Money scale: 2 (SEK)
- Odds scale: 3

---

## Data Model

```mermaid
erDiagram
    CUSTOMERS ||--o{ WALLETS : has
    CUSTOMERS ||--o{ LEDGER_ENTRIES : has
    CUSTOMERS ||--o{ BETS : places

    BETS }o--|| EXPOSURES : contributes_to
    EVENTS ||--o{ BETS : has
    EVENTS ||--|| EVENT_RESULTS : has

    LIMITS ||--o{ EXPOSURES : constrains

    CUSTOMERS {
        bigint id PK
        string name
    }

    LEDGER_ENTRIES {
        bigint id PK
        bigint customer_id FK
        enum type
        decimal amount
        string reference_type
        string reference_id
    }

    BETS {
        bigint id PK
        bigint customer_id FK
        string event_id
        enum status
        decimal stake
        decimal potential_payout
    }

    EXPOSURES {
        bigint id PK
        string event_id
        decimal reserved_liability
        bigint version
    }

    EVENT_RESULTS {
        bigint id PK
        string event_id UK
        enum winning_selection
    }
```

### Key Tables

| Table | Purpose |
|-------|---------|
| `ledger_entries` | Append-only; balance derived by `SUM` (never mutate balance column) |
| `exposures` | Per (eventId, marketType, selection); `version` for optimistic locking |
| `idempotency_keys` | Stores request hash + response JSON for replay detection |

---

## API Reference

### Public Endpoints (JWT)

| Method | Path | Description |
|--------|------|--------------|
| POST | `/api/auth/token` | Get JWT (body: `{"customerId": 1}`) |
| POST | `/api/bets` | Place bet (requires `Idempotency-Key` header) |
| GET | `/api/bets/{id}` | Get bet by ID |
| GET | `/api/customers/{id}/ledger` | Paginated ledger entries |

### Admin Endpoints (Basic Auth)

| Method | Path | Description |
|--------|------|--------------|
| POST | `/admin/events/{eventId}/result` | Post result (requires `Idempotency-Key`) |
| GET | `/admin/exposures` | List exposures (optional `?eventId=`) |
| POST | `/admin/limits` | Set/update risk limits |

### Example: Full Bet Lifecycle

```bash
# 1. Get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1}' | jq -r '.token')

# 2. Place bet
curl -X POST http://localhost:8080/api/bets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: bet-$(date +%s)" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "eventId": "evt-1",
    "marketType": "MATCH_WINNER",
    "selection": "HOME",
    "odds": 1.85,
    "stake": 100
  }'

# 3. Settle (admin)
curl -X POST http://localhost:8080/admin/events/evt-1/result \
  -u admin:admin-secret \
  -H "Idempotency-Key: result-001" \
  -H "Content-Type: application/json" \
  -d '{"winningSelection": "HOME"}'

# 4. Check ledger
curl "http://localhost:8080/api/customers/1/ledger?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Quick Start

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run tests
mvn test

# 3. Run application
mvn spring-boot:run
```

- **Swagger UI**: http://localhost:8080/swagger-ui.html  
- **Actuator**: http://localhost:8080/actuator/health  

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL, Spring Data JPA, Flyway |
| Security | Spring Security (JWT + Basic Auth) |
| API Docs | springdoc OpenAPI |
| Observability | Micrometer, Actuator |
| Testing | JUnit 5, Testcontainers (Postgres) |

---

## Configuration

| Property | Description |
|----------|-------------|
| `jwt.secret` | JWT signing key (min 32 chars) |
| `admin.username` | Admin Basic Auth user |
| `admin.password` | Use `{noop}plain` for dev, bcrypt hash for prod |
