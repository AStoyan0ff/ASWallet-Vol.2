<h1 align="center">
  💰 ASWallet-Vol.2 💰
</h1>

<p align="center">
  <img src="https://spring.io/img/projects/spring-boot.svg" width="120">
</p>

<p align="center">
  Personal FinTech Wallet — Main Application (Spring Boot)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk">
  <img src="https://img.shields.io/badge/version-2.0.2-blue">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot">
  <img src="https://img.shields.io/badge/Spring%20Security-Authentication-success?logo=springsecurity">
  <img src="https://img.shields.io/badge/MySQL-Database-blue?logo=mysql">
  <img src="https://img.shields.io/badge/Thymeleaf-Frontend-green?logo=thymeleaf">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven">
</p>

<h1 align="center">AStoyanoff's Wallet Project</h1>

<p align="center">
  ASWallet-Vol.2 is a full-stack personal wallet platform. Users register, manage a digital wallet,
  link a bank card, receive a welcome bonus, and perform deposits, withdrawals, and transfers.
  The system includes profile management, admin tooling, messaging, scheduling, caching,
  PDF export, legal pages, and a REST risk-assessment microservice integrated via OpenFeign.
</p>

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Tech Stack](#tech-stack)
4. [Domain Model](#domain-model)
5. [Application Features](#application-features)
6. [REST Microservice](#rest-microservice)
7. [Web Pages & Routes](#web-pages--routes)
8. [Security](#security)
9. [Scheduling & Caching](#scheduling--caching)
10. [Email & Events](#email--events)
11. [Error Handling](#error-handling)
12. [Frontend Assets](#frontend-assets)
13. [Complete Project Structure](#complete-project-structure)
14. [Configuration](#configuration)
15. [Getting Started](#getting-started)
16. [Testing & Coverage](#testing--coverage)
17. [For Examiners / Reviewers](#for-examiners--reviewers)
18. [Spring Advanced Assignment](#spring-advanced-assignment)
19. [Version History](#version-history)
20. [Planned Work](#planned-work)
21. [Author](#author)

---

## Overview

ASWallet-Vol.2 is a **two-application system** (per Spring Advanced requirements):

| Application             | Port (default) | Role                                                         |
|-------------------------|----------------|--------------------------------------------------------------|
| **ASWallet-Vol.2 Main** | `8080`         | Thymeleaf UI, wallet logic, Spring Security, MySQL `as_wallet` |
| **REST Microservice**   | `8081`         | Transfer risk assessment API, separate DB `as_wallet_svc`, consumed via **Feign** |

The **main application** (this repository) integrates with the sibling project **`ASWallet-Vol.2-svc`**.

**What is implemented today (v2.0.2):**

- Full wallet lifecycle: register, login, card, deposit, withdraw, transfer (with 2-step confirm)
- Risk-aware transfers via microservice (`ALLOW` / `REVIEW` / `BLOCK`)
- Admin panel: users, roles, status, login audit, mailbox, **risk reviews with history**
- User mailbox, PDF export, settings, profile, daily limit, contact page
- **Privacy Policy** and **Terms of Service** pages
- **Paginated transaction history** (5 rows per page)
- Spring Events (email), scheduling, caching, validation, 70%+ test coverage

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Browser (Thymeleaf UI)                            │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ HTTP
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 ASWallet-Vol.2 Main Application (:8080)                 │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │ Controllers │→ │   Services   │→ │ Repositories│→ │ MySQL         │  │
│  │  (MVC)      │  │  (business)  │  │   (JPA)     │  │ as_wallet     │  │
│  └─────────────┘  └──────────────┘  └─────────────┘  └───────────────┘  │
│         │                  │                                            │
│         │                  ├── Spring Cache                             │
│         │                  ├── @Scheduled jobs                          │
│         │                  └── Application Events → Email (SMTP)        │
│         │                                                               │
│         └── Feign Client (RiskAssessmentClient) ──────────────────┐     │
└───────────────────────────────────────────────────────────────────│─────┘
                                                                    │ REST (no auth today)
                                                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│         ASWallet-Vol.2-svc — Risk Microservice (:8081)                  │
│  POST assess · GET manual-reviews · PATCH review · DELETE → as_wallet_svc │
└─────────────────────────────────────────────────────────────────────────┘
```

**Layered design (main app):**

```
Presentation   →  Controllers, Thymeleaf templates, DTOs
Business       →  Services (Interface + Impl)
Persistence    →  Repositories, JPA Entities, Specifications
Cross-cutting  →  Security, Events, Scheduling, Cache, ExceptionHandler
Integration    →  OpenFeign (risk microservice)
```

---

## Tech Stack

### Main Application

| Layer       | Technology                                                |
|-------------|-----------------------------------------------------------|
| Language    | Java 21                                                   |
| Framework   | Spring Boot 4.0.6                                         |
| Web         | Spring MVC, Thymeleaf                                     |
| Security    | Spring Security (session, CSRF, roles) — **main app only** |
| Persistence | Spring Data JPA, Hibernate                                |
| Database    | MySQL (`as_wallet`)                                       |
| Validation  | Jakarta Bean Validation                                   |
| Mail        | Spring Mail (SMTP — ABV)                                  |
| Events      | Spring `ApplicationEvent` + `@TransactionalEventListener` |
| Scheduling  | `@EnableScheduling`, cron + fixed delay                   |
| Caching     | `@EnableCaching`, `ConcurrentMapCacheManager`             |
| PDF         | OpenPDF 2.0.3                                             |
| Integration | Spring Cloud OpenFeign 2025.1.1, feign-hc5 (PATCH)       |
| Frontend    | HTML5, modular CSS, vanilla JavaScript                    |
| Build       | Maven                                                     |
| Utilities   | Lombok                                                    |

### Microservice (`ASWallet-Vol.2-svc`)

| Layer       | Technology                                         |
|-------------|----------------------------------------------------|
| Framework   | Spring Boot 4.0.6 (separate app)                   |
| API         | Spring Web (REST)                                  |
| Persistence | Spring Data JPA, MySQL `as_wallet_svc`             |
| Entity      | `TransferRiskAssessment`                           |
| Security    | **None** (optional per assignment — see [Security](#security)) |
| Testing     | Unit + integration + WebMvc tests                  |

See: `../ASWallet-Vol.2-svc/README.md`

---

## Domain Model

All entities extend `BaseClass` with **UUID** primary key (`GenerationType.UUID`).

### Entity relationship diagram

```
User (1) ────── (1) Wallet (1) ────── (*) Transaction (*) ────── (1) Wallet
  │                      ▲                                        │
  │                      └──────── sender / receiver ────────────┘
  │
  ├── (1) UserProfileDetails     account status, preferences, daily limit, avatar
  ├── (1) BankCard               last 4 digits, holder, expiry, IBAN
  └── technical: PasswordResetToken, LoginActivity

AdminMailboxMessage              admin ↔ user messaging (recipientUserId → User)
```

### Entities

| Entity                | Table                    | Purpose                                                      |
|-----------------------|--------------------------|--------------------------------------------------------------|
| `User`                | `users`                  | Account: username, email, password (BCrypt), role, createdAt |
| `Wallet`              | `wallets`                | Balance (EUR), linked to one user                            |
| `Transaction`         | `transactions`           | Amount, type, status, description, sender/receiver wallets   |
| `BankCard`            | `bank_cards`             | Card metadata + generated IBAN (no full PAN/CVC stored)      |
| `UserProfileDetails`  | `user_profile_details`   | Profile, account status, settings, daily withdraw limit      |
| `AdminMailboxMessage` | `admin_mailbox_messages` | Two-way admin/user mailbox                                   |
| `PasswordResetToken`  | `password_reset_tokens`  | Forgot-password flow                                         |
| `LoginActivity`       | `login_activity`         | Login audit for admin review                                 |

### Enums

| Enum                | Values                                                    |
|---------------------|-----------------------------------------------------------|
| `UserRole`          | `USER`, `ADMIN`                                           |
| `AccountStatus`     | `ACTIVE`, `INACTIVE`                                      |
| `TransactionType`   | `DEPOSIT`, `WITHDRAW`, `TRANSFER`                         |
| `TransactionStatus` | `COMPLETED`, `PENDING`, `PENDING_RISK_REVIEW`, `FAILED`, `CANCELLED` |
| `SpendingCategory`  | `FOOD`, `SHOPPING`, `BILLS`, `ENTERTAINMENT`, `TRANSPORT` |
| `RiskDecision`      | `ALLOW`, `REVIEW`, `BLOCK` (from microservice)          |
| `AssessmentStatus`  | `PENDING`, `APPROVED`, `REJECTED` (microservice reviews) |

---

## Application Features

### Authentication & account lifecycle

- Registration, login, logout
- BCrypt passwords; CSRF on all POST forms
- Forgot / reset password (email token, SHA-256 stored, expiry, scheduled cleanup)
- Change password (strength meter UI)
- Delete own account (cascade: wallet, card, transactions, profile, tokens, messages)
- Inactive users cannot log in (`AccountStatus.INACTIVE`)

### Wallet operations

- Auto wallet on register (€0.00)
- €50.00 welcome bonus on first bank card registration
- Deposit (card + CVC)
- Withdraw (card; daily limit for non-admin users)
- Transfer by receiver username (2-step confirm; receiver needs card)
- **Risk-aware transfers:** on confirm, main app calls microservice via Feign:
  - **`BLOCK`** — transfer not created; user sees error
  - **`REVIEW`** — transfer saved as `PENDING_RISK_REVIEW`; funds debited; held until admin action
  - **`ALLOW`** — transfer saved as `PENDING`; scheduler auto-completes after processing delay (~5 s)
- Pending transfers: scheduler processes `PENDING` only; `PENDING_RISK_REVIEW` skipped until admin approve/reject
- Stale auto-cancel and user cancel apply to both `PENDING` and `PENDING_RISK_REVIEW`
- **Transaction history** with spending category, clear history, **pagination** (5 per page, Previous/Next)
- Auto-refresh hint when pending transfers exist

### Bank card

- Optional at registration; add later at `/wallet/bank-details`
- Stores: last 4 digits, holder, expiry, IBAN (generated)
- Black/gold card UI; live preview on add; masked view on read

### Profile & settings

- View / edit profile (name, phone, local avatar upload)
- Settings hub: hide balance, email notification toggles
- Daily withdraw limit editor (€50–€500)
- Contact Us page (about text + support details)
- **Data & Privacy:** links to Privacy Policy and Terms of Service

### Admin

- User list with balance, role, status
- Change account status and role (with restriction rules)
- Delete non-admin users
- Per-user manage page
- Login activity audit
- **Risk reviews** (`/admin/risk-reviews`):
  - Lists all manual reviews (`decision=REVIEW`) including **approved/rejected history**
  - Status chips: `PENDING`, `APPROVED`, `REJECTED`
  - Approve/Reject only when status is `PENDING`
  - **Delete all** always visible (top-right); clears list and cancels pending linked transfers
- Mailbox: send to users, inbox, threads

### User mailbox

- Send message to admin, inbox, view thread, delete messages, unread badge on dashboard

### Export

- Filter transactions (type, status, dates) via JPA Specifications
- Preview on page; download PDF (OpenPDF)

### Legal

- **Privacy Policy** (`/wallet/privacy`) — data collection, retention, user rights
- **Terms of Service** (`/wallet/terms`) — ASWallet as financial platform, user liability

---

## REST Microservice

> Sibling project **`ASWallet-Vol.2-svc`** — port `8081`, database `as_wallet_svc`.

### Layout

```
D:\Projects\
├── ASWallet-Vol.2/              ← this repository (main app :8080)
└── ASWallet-Vol.2-svc/          ← risk microservice (:8081)
```

### Feign client (main app)

```java
@FeignClient(name = "aswallet-risk-service", url = "${app.risk-service.base-url}")
public interface RiskAssessmentClient {

    @PostMapping("/api/risk-assessments")
    RiskAssessmentClientResponse createAssessment(@RequestBody RiskAssessmentCreateRequest request);

    @GetMapping("/api/risk-assessments")
    List<RiskAssessmentClientResponse> listAssessments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "decision", required = false) String decision);

    @GetMapping("/api/risk-assessments/manual-reviews")
    List<RiskAssessmentClientResponse> listManualReviews();

    @GetMapping("/api/risk-assessments/{id}")
    RiskAssessmentClientResponse getAssessment(@PathVariable UUID id);

    @PatchMapping("/api/risk-assessments/{id}/review")
    RiskAssessmentClientResponse reviewAssessment(
            @PathVariable UUID id,
            @RequestBody RiskAssessmentReviewRequest request);

    @DeleteMapping("/api/risk-assessments")
    void deleteAssessments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "decision", required = false) String decision);

    @DeleteMapping("/api/risk-assessments/manual-reviews")
    void deleteManualReviews();
}
```

Requires **`feign-hc5`** (`spring.cloud.openfeign.httpclient.hc5.enabled=true`) for `PATCH`.

### Integration flow

1. User confirms transfer → main app pre-generates transaction UUID → Feign **POST** (includes `transactionRef`)
2. Microservice returns decision → main app sets transaction status accordingly
3. Admin opens `/admin/risk-reviews` → Feign **GET** `/manual-reviews` (all `REVIEW` decisions, any status)
4. Admin **Approve** → wallet completes transfer → Feign **PATCH** → `APPROVED`
5. Admin **Reject** → wallet refunds sender → Feign **PATCH** → `REJECTED`
6. Admin **Delete all** → reject pending linked transfers → Feign **DELETE** `/manual-reviews`

Orphaned assessments (MS record without wallet transaction) are handled gracefully: MS status still updates; wallet action skipped with warning log.

### Fail-open behaviour

When microservice is unreachable and `app.risk-service.fail-open=true`, transfers are **allowed without risk check** (`ALLOW` synthetic response). Documented for demo resilience; not ideal for production banking (see [Planned Work](#planned-work)).

### Spring Advanced checklist (microservice)

| Requirement                       | Status |
|-----------------------------------|--------|
| Separate Spring Boot app          | ✅ port `8081` |
| Separate database                 | ✅ `as_wallet_svc` |
| ≥ 1 domain entity in microservice | ✅ `TransferRiskAssessment` |
| ≥ 2 MS functionalities from UI    | ✅ assess on transfer + admin review |
| Feign in main app                 | ✅ `RiskAssessmentClient` |
| ≥ 1 GET + ≥ 2 POST/PATCH/DELETE from main | ✅ |
| 70% test coverage (microservice)  | ✅ unit + WebMvc + integration |

---

## Web Pages & Routes

**33 dynamic pages + 4 Thymeleaf fragments** (37 HTML templates total).

### Public

| Method   | URL                | Template               |
|----------|--------------------|------------------------|
| GET      | `/`                | `home.html`            |
| GET/POST | `/login`           | `login.html`           |
| GET/POST | `/register`        | `register.html`        |
| GET/POST | `/forgot-password` | `forgot-password.html` |
| GET/POST | `/reset-password`  | `reset-password.html`  |
| GET      | `/account-deleted` | `account-deleted.html` |

### Authenticated (USER / ADMIN)

| Method   | URL                              | Template                   |
|----------|----------------------------------|----------------------------|
| GET      | `/wallet`                        | `wallet.html`              |
| GET/POST | `/wallet/bank-details`           | `bank-details.html`        |
| GET/POST | `/wallet/settings`               | `settings.html`            |
| GET/POST | `/wallet/change-password`        | `change-password.html`     |
| GET/POST | `/wallet/delete-account`         | `delete-account.html`      |
| GET/POST | `/wallet/daily-limit/edit`       | `daily-limit-edit.html`    |
| GET      | `/wallet/export`                 | `transaction-export.html`  |
| GET      | `/wallet/export/pdf`             | *(PDF download)*           |
| GET      | `/wallet/contact-us`             | `contact-us.html`          |
| GET      | `/wallet/privacy`                | `privacy-policy.html`      |
| GET      | `/wallet/terms`                  | `terms-of-service.html`    |
| GET/POST | `/wallet/messages/send`          | `wallet-send-message.html` |
| GET      | `/wallet/messages`               | `wallet-messages.html`     |
| GET      | `/wallet/messages/{id}`          | `wallet-message-view.html` |
| GET/POST | `/transactions/deposit`          | `deposit.html`             |
| GET/POST | `/transactions/withdraw`         | `withdraw.html`            |
| GET/POST | `/transactions/transfer`         | `transfer.html`            |
| GET/POST | `/transactions/transfer/confirm` | `transfer-confirm.html`    |
| GET      | `/transactions/history`          | `transaction-history.html` |
| POST     | `/transactions/history/clear`    | redirect                   |
| POST     | `/transactions/history/{id}/cancel` | redirect                |
| GET      | `/profile`                       | `profile.html`             |
| GET/POST | `/profile/edit`                  | `profile-edit.html`        |

### Admin only (`ROLE_ADMIN`)

| Method   | URL                                       | Template                    |
|----------|-------------------------------------------|-----------------------------|
| GET      | `/admin`                                  | `admin.html`                |
| GET      | `/admin/login-activity`                   | `admin-login-activity.html` |
| GET      | `/admin/users/{id}/manage`                | `admin-user-manage.html`    |
| GET/POST | `/admin/messages/send`                    | `admin-send-message.html`   |
| GET      | `/admin/messages/inbox`                   | `admin-message-inbox.html`  |
| GET      | `/admin/messages/users/{username}/thread` | `admin-message-thread.html` |
| GET      | `/admin/risk-reviews`                     | `admin-risk-reviews.html`   |
| POST     | `/admin/risk-reviews/{id}/approve`        | redirect                    |
| POST     | `/admin/risk-reviews/{id}/reject`         | redirect                    |
| POST     | `/admin/risk-reviews/clear`               | redirect (delete all)       |

### Thymeleaf fragments

| Fragment                   | File                                        |
|----------------------------|---------------------------------------------|
| App version footer         | `fragments/app-footer.html`                 |
| Password visibility toggle | `fragments/password-visibility-toggle.html` |
| Spending category field    | `fragments/spending-category-field.html`    |
| Transaction filter fields  | `fragments/transaction-filter-fields.html`  |

---

## Security

### Main application (required — Spring Advanced)

| Mechanism        | Implementation                                              |
|------------------|-------------------------------------------------------------|
| Authentication   | Spring Security form login                                  |
| Password storage | BCrypt (`BCryptPasswordEncoder`)                            |
| Session          | HTTP session + `JSESSIONID`                                 |
| CSRF             | Enabled on all POST forms                                   |
| Roles            | `ROLE_USER`, `ROLE_ADMIN` (UI labels: SUPER / SUPPORT ADMIN) |
| Authorization    | `SecurityConfig` — public paths, `/admin/**` → ADMIN        |
| Account lock     | `INACTIVE` → login blocked                                  |
| Reset tokens     | SHA-256 hash, single-use, expiry                            |
| Card data        | Last 4 + metadata only; CVC never stored                    |
| Role reload      | `ReloadUserAuthoritiesFilter` after promotion               |
| Avatars          | Local upload to `uploads/avatars/` (gitignored)             |
| Secrets          | `MAIL_PASSWORD`, `ADMIN_PASSWORD` via environment variables   |

### Microservice (optional per assignment)

Per `src/main/resources/Spring-Advanced`:

> *Security and Roles — required only for the Main application. The REST microservice(s) may implement security, but it is optional.*

**Current state:** `ASWallet-Vol.2-svc` exposes REST endpoints on `:8081` **without authentication**. Any client that can reach the port may call the API directly (bypassing main-app admin checks). Acceptable for localhost demo; not suitable for exposed/production deployments.

**Planned (see [Planned Work](#planned-work)):** service-to-service protection via shared API key (`X-API-Key` header) between main app (Feign) and microservice.

---

## Scheduling & Caching

### Scheduled jobs

| Class                                                  | Trigger                                               | Purpose                     |
|--------------------------------------------------------|-------------------------------------------------------|-----------------------------|
| `PendingTransferScheduler.processPendingTransfers`     | Cron `app.transfer.process.cron`                      | Process `PENDING` transfers |
| `PendingTransferScheduler.cancelStalePendingTransfers` | Fixed delay `app.transfer.stale-check.fixed-delay-ms` | Cancel stale pending        |
| `PasswordResetTokenCleanupScheduler`                   | Cron `app.password-reset.cleanup.cron`                | Delete expired/used tokens  |

### Cache (`CacheConfig`)

| Cache name           | Used for           |
|----------------------|--------------------|
| `profiles`           | User profile reads |
| `walletSettings`     | Settings reads     |
| `transactionHistory` | History reads      |

Eviction: `ApplicationCacheEviction` on updates and new transactions.

---

## Email & Events

| Event                       | Listener                            | Emails                                                    |
|-----------------------------|-------------------------------------|-----------------------------------------------------------|
| `UserRegisteredEvent`       | `UserRegisteredEventListener`       | Registration confirmation                                 |
| `TransactionCompletedEvent` | `TransactionCompletedEventListener` | Deposit / withdraw / transfer (respects settings toggles) |

SMTP: `smtp.abv.bg:465` — requires `MAIL_PASSWORD` environment variable.

---

## Error Handling

`GlobalExceptionHandler` — unified handling for built-in and **20+ custom domain exceptions**, including:

`InsufficientBalanceException`, `DailyWithdrawLimitExceededException`, `InvalidCardDetailsException`,
`TransferBlockedByRiskException`, `RiskReviewServiceException`, `CannotChangeAdminAccountStatusException`,
`MailboxMessageNotFoundException`, `PendingTransferNotFoundException`, `InvalidAvatarFileException`, and others.

---

## Frontend Assets

### CSS (`static/css/`)

| File                         | Scope                                  |
|------------------------------|----------------------------------------|
| `style.css`                  | Entry — imports all modules            |
| `base/variables.css`         | Design tokens                          |
| `base/reset.css`             | Reset, footer, shared form base        |
| `base/layout-panels.css`     | Panels, backgrounds, shared layout     |
| `base/buttons.css`           | Shared button styles                   |
| `pages/home.css`             | Landing, bank-card login/register      |
| `pages/auth.css`             | Auth forms, materialize animations     |
| `pages/wallet.css`           | Dashboard, balance card, quick actions |
| `pages/transactions.css`     | Deposit, withdraw, transfer, history, pagination |
| `pages/bank-details.css`     | Bank card add/view                     |
| `pages/profile.css`          | Profile, daily limit                   |
| `pages/settings.css`         | Settings hub, toggles                  |
| `pages/admin.css`            | Admin panel, risk reviews, status chips |
| `pages/account-security.css` | Password, delete account               |
| `pages/contact-us.css`       | Contact page                           |
| `pages/legal-pages.css`      | Privacy Policy, Terms of Service       |

### JavaScript (`static/js/`)

| File                     | Purpose                                    |
|--------------------------|--------------------------------------------|
| `home.js`                | Materialize reveal, bank card shine effect |
| `password-toggle.js`     | Show/hide password                         |
| `bank-details.js`        | Card number format, live preview           |
| `wallet-card.js`         | Balance card animations                    |
| `wallet-iban.js`         | IBAN show/hide toggle                      |
| `wallet-settings.js`     | AJAX settings save                         |
| `deposit.js`             | Deposit form helpers                       |
| `change-password.js`     | Password strength meter                    |
| `daily-limit-edit.js`    | Limit stepper UI                           |
| `profile-edit.js`        | Avatar preview                             |
| `transaction-history.js` | Auto-refresh for pending transfers         |
| `admin.js`               | Admin panel interactions                   |

---

## Complete Project Structure

```
ASWallet-Vol.2/
├── pom.xml
├── README.md
├── .gitignore
│
└── src/
    ├── main/
    │   ├── java/STARTER/
    │   │   ├── ASWalletApplication.java
    │   │   ├── Clients/                    # Feign + DTOs
    │   │   ├── Configuration/              (11 config classes)
    │   │   ├── Controllers/                (15 controllers)
    │   │   ├── Services/
    │   │   │   ├── Interface/              (14 interfaces)
    │   │   │   └── Impl/                   (17 implementations)
    │   │   ├── Repositories/               (8 repositories)
    │   │   ├── Models/                     (9 entities)
    │   │   ├── DTOs/
    │   │   ├── Enums/
    │   │   ├── Events/
    │   │   ├── Scheduling/
    │   │   ├── Security/
    │   │   ├── Specifications/
    │   │   ├── CustomException/
    │   │   ├── GlobalExceptionHandler/
    │   │   └── Utils/
    │   └── resources/
    │       ├── application.properties
    │       ├── Spring-Advanced             # Assignment brief
    │       ├── templates/                  # 33 pages + 4 fragments
    │       └── static/
    │           ├── css/                    # 16 files
    │           ├── js/                     # 12 scripts
    │           └── images/
    └── test/
        └── java/STARTER/
            ├── ASWalletApplicationTests.java          # optional smoke (needs MySQL)
            ├── Controllers/                           # 13 @WebMvcTest classes
            ├── GlobalExceptionHandler/
            └── Services/Impl/                         # 11 service unit test classes
```

### Microservice sibling project

```
ASWallet-Vol.2-svc/
├── pom.xml
├── README.md
└── src/main/java/SVC/
    ├── ASWalletSvcApplication.java
    ├── Controllers/RiskAssessmentController.java
    ├── Models/TransferRiskAssessment.java
    ├── Services/RiskScoringService.java
    ├── Services/RiskAssessmentService.java
    ├── Repositories/TransferRiskAssessmentRepository.java
    ├── DTOs/
    ├── Enums/
    ├── Exceptions/
    └── GlobalExceptionHandler/
```

---

## Configuration

### Database (main app)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/as_wallet?createDatabaseIfNotExist=true
spring.datasource.username=your-mysql-username
spring.datasource.password=your-mysql-password
spring.jpa.hibernate.ddl-auto=update
```

Use `application-local.properties` (gitignored) for local credentials.

### Environment variables

| Variable         | Purpose                                     |
|------------------|---------------------------------------------|
| `MAIL_PASSWORD`  | SMTP password for `aswallet.noreply@abv.bg` |
| `ADMIN_PASSWORD` | Default admin password on first bootstrap   |
| `ADMIN_CARD_*`   | Optional admin bootstrap card overrides     |

### Key properties

```properties
spring.application.name=ASWallet-Vol.2
app.version=2.0.2
app.base-url=http://localhost:8080
server.address=0.0.0.0          # LAN access (phone testing via http://<PC-IP>:8080)
app.transfer.processing-delay-seconds=5
app.transfer.process.cron=0 */1 * * * *
app.withdraw.daily-limit.min=50
app.withdraw.daily-limit.max=500
app.withdraw.day-timezone=Europe/Sofia
# Risk microservice (ASWallet-Vol.2-svc)
app.risk-service.enabled=true
app.risk-service.base-url=http://localhost:8081
app.risk-service.fail-open=true
spring.cloud.openfeign.httpclient.hc5.enabled=true
```

For phone/LAN testing, point `app.risk-service.base-url` to the PC LAN IP (not `localhost`) on the device.

---

## Getting Started

### Prerequisites

- Java 21
- Maven
- MySQL

### Run main application

```bash
git clone https://github.com/AStoyan0ff/ASWallet-Vol.2.git
cd ASWallet-Vol.2
```

```powershell
$env:MAIL_PASSWORD = "your-smtp-password"
mvn spring-boot:run
```

Open: **http://localhost:8080**

Main class: `STARTER.ASWalletApplication`

### Run risk microservice (required for transfer risk checks)

```powershell
cd ..\ASWallet-Vol.2-svc
$env:DB_PASSWORD = "your_mysql_password"
mvn spring-boot:run
```

Microservice: **http://localhost:8081**

If the microservice is down and `app.risk-service.fail-open=true`, transfers still work (risk check skipped).

---

## Testing & Coverage

JUnit 5, Mockito, AssertJ, Spring Boot `@WebMvcTest` (with `spring-security-test`).

### Summary

| Metric | Value |
|--------|-------|
| Test classes | **25** (+ optional `ASWalletApplicationTests`) |
| Line coverage (JaCoCo, main app) | **~77%** (target 70% ✅) |
| Microservice coverage | ✅ unit + integration + WebMvc |

### Run tests

```powershell
mvn test "-Dtest=!ASWalletApplicationTests"
mvn test "-Dtest=TransactionHistoryControllerWebMvcTest"
```

### JaCoCo report

```powershell
mvn org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.13:report "-Dtest=!ASWalletApplicationTests"
```

Open: `target/site/jacoco/index.html`

### Unit tests (`Services/Impl`)

`TransactionServiceImplTest`, `UserServiceImplTest`, `WalletServiceImplTest`, `BankCardServiceImplTest`,
`PasswordResetServiceImplTest`, `PendingTransferProcessingServiceTest`, `AdminRiskReviewServiceImplTest`,
`AdminServiceImplTest`, `AdminMailboxServiceImplTest`, `UserProfileDetailsServiceImplTest`,
`WithdrawDailyLimitServiceImplTest`

### WebMvc tests (`Controllers`)

`UserControllerWebMvcTest`, `PasswordResetControllerWebMvcTest`, `ProfileControllerWebMvcTest`,
`WalletControllerWebMvcTest`, `WalletMailboxControllerWebMvcTest`, `AdminControllerWebMvcTest`,
`AdminRiskReviewControllerWebMvcTest`, `AdminMailboxControllerWebMvcTest`,
`TransactionControllerDepositWebMvcTest`, `TransactionControllerWithdrawWebMvcTest`,
`TransactionControllerTransferWebMvcTest`, `LegalPagesControllerWebMvcTest`,
`TransactionHistoryControllerWebMvcTest`

### Other

`GlobalExceptionHandlerTest`

### Controllers without dedicated MockMvc yet

`HomeController`, `ContactUsController`, `DailyLimitController`, `TransactionExportController` — low priority; coverage target met.

---

## For Examiners / Reviewers

### Main flow (5 min)

1. Register → Login → Wallet (€0.00)
2. Bank Details → add card → €50 bonus
3. Deposit → Withdraw → Transfer (confirm flow; trigger risk review with night/large amount)
4. History → pagination → Export PDF
5. Settings → Privacy / Terms → Profile → Contact Us
6. Admin: `/admin` — users, mailbox, login activity, **risk reviews** (approve/reject/history/delete all)

### Test card

`4111 1111 1111 1111` · any future expiry · CVC `123`

### Default admin

| Field    | Value                            |
|----------|----------------------------------|
| Username | `admin`                          |
| Password | `admin123` (or `ADMIN_PASSWORD`) |

### Troubleshooting

| Issue                | Fix                             |
|----------------------|---------------------------------|
| DB connection failed | Start MySQL; check credentials  |
| No emails            | Set `MAIL_PASSWORD`             |
| Transfer blocked     | Receiver needs bank card; or risk score ≥ 70 |
| Risk review stuck    | Start microservice on `:8081`; admin at `/admin/risk-reviews` |
| Phone cannot connect | Use `http://<PC-LAN-IP>:8080`; set `app.risk-service.base-url` to same IP for svc |
| `Data truncated for column 'status'` | Run migration below if upgrading DB |
| Inactive login       | Admin reactivates from `/admin` |

### Database migration (existing installs)

```sql
USE as_wallet;
ALTER TABLE transactions MODIFY COLUMN status VARCHAR(32) NOT NULL;
```

---

## Spring Advanced Assignment

Official brief: `src/main/resources/Spring-Advanced`

### Main app — requirement checklist

| Requirement                      | Status         |
|----------------------------------|----------------|
| ≥ 3 domain entities              | ✅              |
| ≥ 10 web pages (≥ 9 dynamic)     | ✅ 33 pages     |
| ≥ 6 valid domain functionalities | ✅              |
| Spring Security + 2 roles        | ✅              |
| Admin manages roles              | ✅              |
| View/edit own profile            | ✅              |
| UUID PKs                         | ✅              |
| Entity relationships             | ✅              |
| Scheduling (cron + non-cron)     | ✅              |
| Caching                          | ✅              |
| Validation + error handling      | ✅              |
| PDF export (bonus)               | ✅              |
| REST microservice + Feign        | ✅              |
| 70% test coverage (main app)     | ✅ ~77% JaCoCo  |
| 70% test coverage (microservice) | ✅              |
| Separate MS database             | ✅ `as_wallet_svc` |

### Valid domain functionalities (main app)

| # | Action                  | Result                     |
|---|-------------------------|----------------------------|
| 1 | Register bank card      | Card + bonus + transaction |
| 2 | Deposit                 | Balance ↑                  |
| 3 | Withdraw                | Balance ↓                  |
| 4 | Confirm transfer        | Risk assessed via Feign; balances updated |
| 5 | Cancel pending transfer | Refund + status change     |
| 6 | Send mailbox message    | Message persisted          |
| 7 | Export PDF              | Filtered download          |
| 8 | Admin risk review       | Approve/reject/clear flagged transfer |

### Valid domain functionalities (microservice via main UI)

| # | Action (UI)               | Feign | MS state change        |
|---|---------------------------|-------|------------------------|
| 1 | Confirm transfer          | POST  | New risk assessment    |
| 2 | Admin approve risk review | PATCH | Assessment → APPROVED  |
| 3 | Admin reject risk review  | PATCH | Assessment → REJECTED  |
| 4 | Admin delete all reviews  | DELETE | Manual reviews cleared |

---

## Version History

### v2.0.2 (current)

- **Risk review history:** `/admin/risk-reviews` lists all manual reviews (`GET /manual-reviews`); approved/rejected stay visible until Delete all; status chips; Approve/Reject hidden when closed
- **Transaction history pagination:** 5 rows per page, Previous/Next
- **Legal pages:** Privacy Policy (`/wallet/privacy`), Terms of Service (`/wallet/terms`); links from Settings → Data & Privacy
- **Feign:** `listManualReviews`, `deleteManualReviews`, `decision` filter on list/delete
- **Tests:** `LegalPagesControllerWebMvcTest`, `TransactionHistoryControllerWebMvcTest`; updated risk review tests

### v2.0.1

- End-to-end `PENDING_RISK_REVIEW` flow, admin risk UI, elevated wallet/admin tiles
- Feign `getAssessment`, `deleteAssessments`; risk-held approve/reject in scheduler

---

## Planned Work

| Item | Priority | Notes |
|------|----------|-------|
| **Microservice security (API key)** | High | `app.risk-service.api-key` + `X-API-Key` header via Feign interceptor; filter in svc. Optional per assignment; recommended before non-localhost deployment. |
| **Fail-open policy** | Medium | Replace boolean with modes: `allow` (demo), `block`, `review` (hold as `PENDING_RISK_REVIEW` when svc down) |
| **Mobile-responsive home** | Medium | Viewport meta, scale hero/cards for iPhone and small screens |
| **DB secrets via env** | Medium | Move MySQL passwords to `${DB_PASSWORD}` (main + svc) |
| Edit / replace bank card | Low | UX improvement |
| OTP / 2FA | Low | Security enhancement (bonus-eligible) |
| JWT service auth | Low | Alternative to API key; bonus-eligible |

---

## Author

Developed by **AStoyanoff®**
