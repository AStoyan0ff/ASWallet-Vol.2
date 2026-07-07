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
  <img src="https://img.shields.io/badge/version-2.0.1-blue">
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
  PDF export, and a planned REST microservice integrated via OpenFeign.
</p>

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Tech Stack](#tech-stack)
4. [Domain Model](#domain-model)
5. [Application Features](#application-features)
6. [REST Microservice (planned)](#rest-microservice-planned)
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
19. [Version](#version)
20. [Planned Work](#planned-work)
21. [Author](#author)

---

## Overview

ASWallet-Vol.2 is designed as a **two-application system** (per Spring Advanced requirements):

| Application                       | Port (default) | Role                                                         |
|-----------------------------------|----------------|--------------------------------------------------------------|
| **ASWallet-Vol.2 Main**           | `8080`         | Thymeleaf UI, wallet logic, security, MySQL `as_wallet`      |
| **REST Microservice** *(planned)* | `8081`         | REST API, separate DB, consumed by Main via **Feign Client** |

The **main application** (this repository) is ready for local demo and defense.
The **microservice module** will be added as a separate Spring Boot app with its own database.

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
│         └── Feign Client (planned) ───────────────────────────────┐     │
└───────────────────────────────────────────────────────────────────│─────┘
                                                                    │ REST
                                                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              REST Microservice (:8081) — planned                        │
│  REST Controllers → Services → JPA → MySQL (separate database)           │
└─────────────────────────────────────────────────────────────────────────┘
```

**Layered design (main app):**

```
Presentation   →  Controllers, Thymeleaf templates, DTOs
Business       →  Services (Interface + Impl)
Persistence    →  Repositories, JPA Entities, Specifications
Cross-cutting  →  Security, Events, Scheduling, Cache, ExceptionHandler
```

---

## Tech Stack

### Main Application (implemented)

| Layer       | Technology                                                |
|-------------|-----------------------------------------------------------|
| Language    | Java 21                                                   |
| Framework   | Spring Boot 4.0.6                                         |
| Web         | Spring MVC, Thymeleaf                                     |
| Security    | Spring Security (session, CSRF, roles)                    |
| Persistence | Spring Data JPA, Hibernate                                |
| Database    | MySQL                                                     |
| Validation  | Jakarta Bean Validation                                   |
| Mail        | Spring Mail (SMTP — ABV)                                  |
| Events      | Spring `ApplicationEvent` + `@TransactionalEventListener` |
| Scheduling  | `@EnableScheduling`, cron + fixed delay                   |
| Caching     | `@EnableCaching`, `ConcurrentMapCacheManager`             |
| PDF         | OpenPDF 2.0.3                                             |
| Frontend    | HTML5, modular CSS, vanilla JavaScript                    |
| Build       | Maven                                                     |
| Utilities   | Lombok                                                    |

### Microservice (planned)

| Layer       | Technology                                         |
|-------------|----------------------------------------------------|
| Framework   | Spring Boot 4.0.6 (separate module)                |
| API         | Spring Web (REST)                                  |
| Integration | **Spring Cloud OpenFeign** (in main app)           |
| Database    | MySQL (separate schema)                            |
| Testing     | Unit, integration, API tests (70% coverage target) |

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
| `TransactionStatus` | `COMPLETED`, `PENDING`, `FAILED`, `CANCELLED`             |
| `SpendingCategory`  | `FOOD`, `SHOPPING`, `BILLS`, `ENTERTAINMENT`, `TRANSPORT` |

---

## Application Features

### Authentication & account lifecycle

- Registration, login, logout
- BCrypt passwords; CSRF on all POST forms
- Forgot / reset password (email token, SHA-256 stored, expiry, scheduled cleanup)
- Change password
- Delete own account (cascade: wallet, card, transactions, profile, tokens, messages)
- Inactive users cannot log in (`AccountStatus.INACTIVE`)

### Wallet operations

- Auto wallet on register (€0.00)
- €50.00 welcome bonus on first bank card registration
- Deposit (card + CVC)
- Withdraw (card; daily limit for non-admin users)
- Transfer by receiver username (2-step confirm; receiver needs card)
- Pending transfers: scheduler processes → `COMPLETED` / `FAILED`; stale auto-cancel; user cancel
- Transaction history, clear history, spending category per operation

### Bank card

- Optional at registration; add later at `/wallet/bank-details`
- Stores: last 4 digits, holder, expiry, IBAN (generated)
- Black/gold card UI; live preview on add; masked view on read

### Profile & settings

- View / edit profile (name, phone, local avatar upload)
- Settings hub: hide balance, email notification toggles
- Daily withdraw limit editor (€50–€500)
- Contact Us page (about text + support details)

### Admin

- User list with balance, role, status
- Change account status and role (with restriction rules)
- Delete non-admin users
- Per-user manage page
- Login activity audit
- Mailbox: send to users, inbox, threads

### User mailbox

- Send message to admin, inbox, view thread, delete messages, unread badge on dashboard

### Export

- Filter transactions (type, status, dates) via JPA Specifications
- Preview on page; download PDF (OpenPDF)

---

## REST Microservice (planned)

> Required by the **Spring Advanced** assignment. Not yet in this repository.

### Planned layout

```
ASWallet-Vol.2/                    (monorepo or multi-module — TBD)
├── main/                          ← current application (this codebase)
└── aswallet-microservice/         ← separate Spring Boot app
    ├── src/main/java/...
    ├── src/main/resources/application.properties   (port 8081, own DB)
    └── pom.xml
```

### Integration (main app)

```java

@FeignClient(name = "aswallet-microservice", url = "${app.microservice.base-url}")
public interface AsWalletMicroserviceClient {
    // ≥ 1 GET + ≥ 2 POST/PUT/DELETE used by main app
}
```

### Planned requirements

| Requirement                       | Plan                                             |
|-----------------------------------|--------------------------------------------------|
| Separate Spring Boot app          | Own `main`, port `8081`                          |
| Separate database                 | e.g. `as_wallet_svc`                             |
| ≥ 1 domain entity in microservice | TBD (e.g. fraud check / audit service)           |
| ≥ 2 valid functionalities in MS   | Triggered from main UI → Feign → MS state change |
| Feign in main app                 | Spring Cloud OpenFeign + client interface        |
| 70% test coverage                 | Unit + integration + API in both apps            |

---

## Web Pages & Routes

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
| GET/POST | `/wallet/messages/send`          | `wallet-send-message.html` |
| GET      | `/wallet/messages`               | `wallet-messages.html`     |
| GET      | `/wallet/messages/{id}`          | `wallet-message-view.html` |
| GET/POST | `/transactions/deposit`          | `deposit.html`             |
| GET/POST | `/transactions/withdraw`         | `withdraw.html`            |
| GET/POST | `/transactions/transfer`         | `transfer.html`            |
| GET/POST | `/transactions/transfer/confirm` | `transfer-confirm.html`    |
| GET      | `/transactions/history`          | `transaction-history.html` |
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

### Thymeleaf fragments

| Fragment                   | File                                        |
|----------------------------|---------------------------------------------|
| App version footer         | `fragments/app-footer.html`                 |
| Password visibility toggle | `fragments/password-visibility-toggle.html` |
| Spending category field    | `fragments/spending-category-field.html`    |
| Transaction filter fields  | `fragments/transaction-filter-fields.html`  |

---

## Security

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
| Secrets          | `MAIL_PASSWORD`, `ADMIN_PASSWORD` via environment variables |

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
`CannotChangeAdminAccountStatusException`, `MailboxMessageNotFoundException`, `PendingTransferNotFoundException`,
`InvalidAvatarFileException`, and others under `CustomException/`.

---

## Frontend Assets

### CSS (`static/css/`)

| File                         | Scope                                  |
|------------------------------|----------------------------------------|
| `style.css`                  | Entry — imports all modules            |
| `base/variables.css`         | Design tokens                          |
| `base/reset.css`             | Reset, footer, shared form base        |
| `base/layout-panels.css`     | Panels, backgrounds, shared layout     |
| `pages/home.css`             | Landing, bank-card login/register      |
| `pages/auth.css`             | Auth forms, materialize animations     |
| `pages/wallet.css`           | Dashboard, balance card, quick actions |
| `pages/transactions.css`     | Deposit, withdraw, transfer, history   |
| `pages/bank-details.css`     | Bank card add/view                     |
| `pages/profile.css`          | Profile, daily limit                   |
| `pages/settings.css`         | Settings hub, toggles                  |
| `pages/admin.css`            | Admin panel, mailbox, login activity   |
| `pages/account-security.css` | Password, delete account               |
| `pages/contact-us.css`       | Contact page                           |

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

### Images (`static/images/`)

`2.png`, `4.png`, `5.png`, `wallet.png`, `log-reg.png`, `res-pass.png`, `transfer.png`, `bank-details.png`,
`userDetails.png`, `editDetails.png`, `setings.png`, `change-password.png`, `delete-account.png`, `daily-limit.png`,
`export-file.png`, `contactUs.png`, `mailbox.png`, `emv-chip.png`, `noContact.png`, `default-avatar.svg`

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
    │   │   ├── Configuration/
    │   │   │   ├── AdminBootstrapConfig.java
    │   │   │   ├── AppModelAdvice.java
    │   │   │   ├── AsyncConfig.java
    │   │   │   ├── AvatarUploadConfig.java
    │   │   │   ├── BankCardIbanBackfillConfig.java
    │   │   │   ├── CacheConfig.java
    │   │   │   ├── SchedulingConfig.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── SupportMailboxBackfillConfig.java
    │   │   │   ├── WalletBalanceBackfillConfig.java
    │   │   │   └── WithdrawDailyLimitBackfillConfig.java
    │   │   ├── Controllers/              (13 controllers)
    │   │   ├── Services/
    │   │   │   ├── Interface/            (13 interfaces)
    │   │   │   └── Impl/                 (16 implementations)
    │   │   ├── Repositories/             (8 repositories)
    │   │   ├── Models/                   (9 entities)
    │   │   ├── DTOs/                     (25 DTOs)
    │   │   ├── Enums/                    (5 enums)
    │   │   ├── Events/                   (4 event classes)
    │   │   ├── Scheduling/               (2 schedulers)
    │   │   ├── Security/                 (2 classes)
    │   │   ├── Specifications/           (TransactionSpecifications)
    │   │   ├── CustomException/          (20 exceptions)
    │   │   ├── GlobalExceptionHandler/
    │   │   └── Utils/                    (5 utilities)
    │   └── resources/
    │       ├── application.properties
    │       ├── Spring-Advanced             # Assignment brief
    │       ├── templates/                  # 31 pages + 4 fragments
    │       └── static/
    │           ├── css/                    # 14 files (base + pages)
    │           ├── js/                     # 12 scripts
    │           └── images/                 # 20 assets
    └── test/
        └── java/STARTER/
            ├── ASWalletApplicationTests.java          # optional smoke (needs MySQL)
            ├── Controllers/                           # 11 @WebMvcTest classes
            ├── GlobalExceptionHandler/
            │   └── GlobalExceptionHandlerTest.java
            └── Services/Impl/                         # 10 service unit test classes
```

### Planned addition (microservice)

```
aswallet-microservice/                       # separate module — to be added
├── pom.xml
└── src/main/java/.../
    ├── *Application.java
    ├── controller/
    ├── model/
    ├── repository/
    ├── service/
    └── dto/
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
app.version=2.0.1
app.base-url=http://localhost:8080
app.admin.username=admin
app.transfer.processing-delay-seconds=5
app.transfer.process.cron=0 */1 * * * *
app.withdraw.daily-limit.min=50
app.withdraw.daily-limit.max=500
app.withdraw.day-timezone=Europe/Sofia
# Microservice (planned)
# app.microservice.base-url=http://localhost:8081
```

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

---

## Testing & Coverage

Automated tests for the **main application** use JUnit 5, Mockito, AssertJ, and Spring Boot `@WebMvcTest` (with `spring-security-test`).

### Summary

| Metric | Value |
|--------|-------|
| Test classes | **20** (+ optional `ASWalletApplicationTests`) |
| Test methods | **~244** |
| Line coverage (JaCoCo) | **~77%** (target 70% ✅ for main app) |
| Microservice coverage | ⏳ planned when MS is added |

### Run tests

```powershell
# All tests except contextLoads (no live MySQL required for the suite)
mvn test "-Dtest=!ASWalletApplicationTests"

# Single class
mvn test "-Dtest=WalletControllerWebMvcTest"
```

### JaCoCo report

```powershell
mvn org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.13:report "-Dtest=!ASWalletApplicationTests"
```

Open: `target/site/jacoco/index.html`

### Unit tests (`Services/Impl`)

| Test class | Focus |
|------------|-------|
| `TransactionServiceImplTest` | deposit, withdraw, transfer, welcome bonus |
| `UserServiceImplTest` | register, password, delete account |
| `WalletServiceImplTest` | wallet CRUD / balance |
| `BankCardServiceImplTest` | card save, IBAN, welcome bonus |
| `PasswordResetServiceImplTest` | forgot / reset token flow |
| `PendingTransferProcessingServiceTest` | async transfer processing |
| `AdminServiceImplTest` | admin user management |
| `AdminMailboxServiceImplTest` | admin ↔ user messaging |
| `UserProfileDetailsServiceImplTest` | profile, settings, account status |
| `WithdrawDailyLimitServiceImplTest` | daily limit rules |

### WebMvc tests (`Controllers`)

| Test class | Controller |
|------------|------------|
| `UserControllerWebMvcTest` | login, register |
| `PasswordResetControllerWebMvcTest` | forgot / reset password |
| `ProfileControllerWebMvcTest` | profile view / edit |
| `WalletControllerWebMvcTest` | wallet, bank card, settings, delete account |
| `WalletMailboxControllerWebMvcTest` | user mailbox |
| `AdminControllerWebMvcTest` | admin dashboard, users, roles |
| `AdminMailboxControllerWebMvcTest` | admin mailbox |
| `TransactionControllerDepositWebMvcTest` | deposit |
| `TransactionControllerWithdrawWebMvcTest` | withdraw |
| `TransactionControllerTransferWebMvcTest` | transfer + confirm |

### Other

| Test class | Focus |
|------------|-------|
| `GlobalExceptionHandlerTest` | centralized error handling |

### Controllers without MockMvc yet

`HomeController`, `ContactUsController`, `DailyLimitController`, `TransactionHistoryController`, `TransactionExportController` — low priority; main app coverage target is already met.

### Test dependencies (`pom.xml`)

- `spring-boot-starter-webmvc-test`
- `spring-security-test` / `spring-boot-starter-security-test`
- `mockito-junit-jupiter`, `assertj-core`

---

## For Examiners / Reviewers

### Main flow (5 min)

1. Register → Login → Wallet (€0.00)
2. Bank Details → add card → €50 bonus
3. Deposit → Withdraw → Transfer (confirm flow)
4. History → Export PDF
5. Settings → Profile → Contact Us
6. Admin: `/admin` — status, role, mailbox, login activity

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
| Transfer blocked     | Receiver needs bank card        |
| Inactive login       | Admin reactivates from `/admin` |

---

## Spring Advanced Assignment

Official brief: `src/main/resources/Spring-Advanced`

### Main app — requirement checklist

| Requirement                      | Status         |
|----------------------------------|----------------|
| ≥ 3 domain entities              | ✅              |
| ≥ 10 web pages (≥ 9 dynamic)     | ✅ 31 templates |
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
| REST microservice + Feign        | ⏳ Planned      |
| 70% test coverage (main app)     | ✅ ~77% JaCoCo  |
| 70% test coverage (microservice) | ⏳ Planned      |
| Separate MS database             | ⏳ Planned      |

### Valid domain functionalities (main app)

| # | Action                  | Result                     |
|---|-------------------------|----------------------------|
| 1 | Register bank card      | Card + bonus + transaction |
| 2 | Deposit                 | Balance ↑                  |
| 3 | Withdraw                | Balance ↓                  |
| 4 | Confirm transfer        | Balances updated           |
| 5 | Cancel pending transfer | Refund + status change     |
| 6 | Send mailbox message    | Message persisted          |
| 7 | Export PDF              | Filtered download          |

---

## Version

**2.0.1** — `pom.xml` · `app.version` · footer on all pages.

---

## Planned Work

| Item                                 | Notes                |
|--------------------------------------|----------------------|
| **REST microservice + Feign Client** | Separate app, own DB |
| **70% test coverage (microservice)** | Main app done (~77%) |
| Edit / replace bank card             | UX improvement       |
| OTP / 2FA                            | Security enhancement |
| Desktop dashboard redesign           | Future UI variant    |

---

## Author

Developed by **AStoyanoff®**
