<h1 align="center">
  💰 ASWallet-Vol.2 💰
</h1>

<p align="center">
  <img src="src/main/resources/static/images/asset/screenShot.png" width="420" alt="ASWallet">
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

<h1 align="center">ASWallet-Vol.2 Project</h1>

<p align="center">
  Full-stack personal wallet platform with Thymeleaf UI, Spring Security, scheduling, caching,
  PDF export, admin tooling, messaging, legal pages, and a REST risk-assessment microservice
  (<code>ASWallet-Vol.2-svc</code>) integrated via OpenFeign.
</p>

---

## Table of Contents

1. [Project Inventory](#project-inventory)
2. [Overview](#overview)
3. [System Architecture](#system-architecture)
4. [Tech Stack](#tech-stack)
5. [Domain Model](#domain-model)
6. [Application Features](#application-features)
7. [REST Microservice Integration](#rest-microservice-integration)
8. [Web Pages & Routes](#web-pages--routes)
9. [Security](#security)
10. [Scheduling & Caching](#scheduling--caching)
11. [Email & Events](#email--events)
12. [Error Handling](#error-handling)
13. [Frontend Assets](#frontend-assets)
14. [Backend Source Inventory](#backend-source-inventory)
15. [Configuration](#configuration)
16. [Getting Started](#getting-started)
17. [Testing & Coverage](#testing--coverage)
18. [For Examiners / Reviewers](#for-examiners--reviewers)
19. [Spring Advanced Assignment](#spring-advanced-assignment)
20. [Version History](#version-history)
21. [Author](#author)

---

## Project Inventory

- Snapshot of the **current codebase** (main app `ASWallet-Vol.2`)
- Microservice: **`../ASWallet-Vol.2-svc`** - https://github.com/AStoyan0ff/ASWallet-Vol.2-svc.git

| Area | Count      | Notes                                          |
|------|------------|------------------------------------------------|
| Java source files | **159**    | `src/main/java/STARTER/`                       |
| Controllers | **15**     | Spring MVC                                     |
| Service interfaces | **15**     | `Services/Interface/`                          |
| Service implementations | **20**     | `Services/Impl/` (incl. support classes)       |
| JPA repositories | **8**      |                                                |
| JPA entities | **9**      | incl. `BaseClass`                              |
| DTOs | **27**     | `DTOs/`                                        |
| Enums | **8**      | `Enums/`                                       |
| Custom exceptions | **26**     | `CustomException/`                             |
| Configuration classes | **11**     | `Configuration/`                               |
| Feign client files | **4**      | client + 3 DTOs                                |
| Thymeleaf pages | **33**     | dynamic/full pages                             |
| Thymeleaf fragments | **4**      | reusable partials                              |
| HTML templates total | **37**     | pages + fragments                              |
| CSS files | **16**     | `static/css/`                                  |
| JavaScript files | **17**     | `static/js/`                                   |
| Test classes | **26**     | **272 +-** test methods (excl. optional smoke) |
| Line coverage (JaCoCo) | **77% ++** | target 70% ✅                                   |

Sibling microservice: **`../ASWallet-Vol.2-svc`** — see its README.

---

## Overview

Two independent Spring Boot applications (Spring Advanced requirement):

| Application | Port | Database | Role |
|-------------|------|----------|------|
| **ASWallet-Vol.2** (this repo) | `8080` | `as_wallet` | UI, wallet logic, security, Feign client |
| **ASWallet-Vol.2-svc** | `8081` | `as_wallet_svc` | Transfer risk scoring REST API |

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
│  Controllers → Services → Repositories → MySQL (as_wallet)              │
│       │              ├── Cache · Scheduling · Events → SMTP             │
│       └── Feign (RiskAssessmentClient) ─────────────────────────────┐     │
└───────────────────────────────────────────────────────────────────│─────┘
                                                                    │ REST
                                                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│         ASWallet-Vol.2-svc — Risk Microservice (:8081)                  │
│  RiskAssessmentController → Services → JPA → MySQL (as_wallet_svc)      │
└─────────────────────────────────────────────────────────────────────────┘
```

**Layers (main app):** Presentation (Controllers, Thymeleaf, DTOs) → Business (Services) → Persistence (Repositories, Entities) → Cross-cutting (Security, Events, Cache, Scheduling, ExceptionHandler, Feign).

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring MVC, Thymeleaf |
| Security | Spring Security (session, CSRF, roles) |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL (`as_wallet`) |
| Validation | Jakarta Bean Validation |
| Mail | Spring Mail (SMTP — ABV) |
| Events | Spring `ApplicationEvent` + `@TransactionalEventListener` |
| Scheduling | `@EnableScheduling` — cron + fixed delay |
| Caching | `@EnableCaching`, `ConcurrentMapCacheManager` |
| PDF | OpenPDF 2.0.3 |
| Integration | Spring Cloud OpenFeign 2025.1.1, feign-hc5 |
| Frontend | HTML5, **16 CSS** + **16 JS** modules, vanilla JS |
| Build | Maven |
| Utilities | Lombok |

---

## Domain Model

All entities extend `BaseClass` with **UUID** primary key.

### Relationships

```
User (1) ── (1) Wallet (1) ── (*) Transaction (*) ── (1) Wallet
  │                   ▲              sender / receiver
  ├── (1) UserProfileDetails
  ├── (1) BankCard
  └── technical: PasswordResetToken, LoginActivity

AdminMailboxMessage → User (recipient)
```

### Entities (9)

| Entity | Table | Type | Purpose                                |
|--------|-------|------|----------------------------------------|
| `User` | `users` | Domain | Account, BCrypt password, role         |
| `Wallet` | `wallets` | Domain | Balance (EUR)                          |
| `Transaction` | `transactions` | Domain | Deposit / withdraw / transfer          |
| `BankCard` | `bank_cards` | Domain | Card metadata, IBAN                    |
| `UserProfileDetails` | `user_profile_details` | Domain | Profile, status, settings, daily limit |
| `AdminMailboxMessage` | `admin_mailbox_messages` | Domain | Admin <-> user messaging               |
| `PasswordResetToken` | `password_reset_tokens` | Technical | Forgot-password tokens                 |
| `LoginActivity` | `login_activity` | Technical | Login audit                            |
| `BaseClass` | — | Mapped superclass | UUID `id`                              |

### Enums (8)

| Enum | Values |
|------|--------|
| `UserRole` | `USER`, `ADMIN` |
| `AccountStatus` | `ACTIVE`, `INACTIVE` |
| `TransactionType` | `DEPOSIT`, `WITHDRAW`, `TRANSFER` |
| `TransactionStatus` | `COMPLETED`, `PENDING`, `PENDING_RISK_REVIEW`, `FAILED`, `CANCELLED` |
| `SpendingCategory` | `FOOD`, `SHOPPING`, `BILLS`, `ENTERTAINMENT`, `TRANSPORT` |
| `RiskDecision` | `ALLOW`, `REVIEW`, `BLOCK` |
| `AssessmentStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `RiskLevel` | `LOW`, `MEDIUM`, `HIGH` |

---

## Application Features

### Authentication & account

- Register, login, logout, forgot/reset password, change password, delete account
- BCrypt passwords, CSRF on POST, inactive account lock
- Password reset: SHA-256 token hash, expiry, scheduled cleanup

### Wallet & transactions

- Auto wallet on register; €50 welcome bonus on first card
- Deposit, withdraw (daily limit), transfer (2-step confirm)
- **Risk-aware transfers** via Feign → `BLOCK` / `REVIEW` / `ALLOW`
- Pending transfer scheduler; user cancel; stale auto-cancel
- Transaction history with **pagination** (5/page), spending categories, clear history
- PDF export with JPA Specifications filters

### Bank card

- Add at register or `/wallet/bank-details`; live preview; masked display; generated IBAN

### Profile & settings

- View/edit profile, avatar upload, settings toggles, daily limit editor
- Contact Us; **Privacy Policy**; **Terms of Service**

### Admin

- User management (status, role, delete), login activity audit
- **Risk reviews** with history, status chips, approve/reject/delete all
- Admin mailbox (send, inbox, threads)

### User mailbox

- Send to admin, inbox, view/delete messages, unread badge

---

## REST Microservice Integration

Sibling: **`ASWallet-Vol.2-svc`** (`:8081`, `as_wallet_svc`). Full API docs: `../ASWallet-Vol.2-svc/README.md`.

---

### Flow summary

| Decision | Wallet status | Behaviour |
|----------|---------------|-----------|
| `ALLOW` | `PENDING` | Scheduler completes (~5 s) |
| `REVIEW` | `PENDING_RISK_REVIEW` | Held until admin approve/reject |
| `BLOCK` | — | Transfer not created |

Admin list uses `GET /manual-reviews` (all `REVIEW` decisions, any status). **Delete all** → `DELETE /manual-reviews`.

`app.risk-service.fail-open=true` → if svc down, transfers allowed without check

---

## Web Pages & Routes

### All Thymeleaf pages (33)

| # | Template | Route(s) | Access |
|---|----------|----------|--------|
| 1 | `home.html` | `/` | Public |
| 2 | `login.html` | `/login` | Public |
| 3 | `register.html` | `/register` | Public |
| 4 | `forgot-password.html` | `/forgot-password` | Public |
| 5 | `reset-password.html` | `/reset-password` | Public |
| 6 | `account-deleted.html` | `/account-deleted` | Public |
| 7 | `wallet.html` | `/wallet` | Auth |
| 8 | `bank-details.html` | `/wallet/bank-details` | Auth |
| 9 | `settings.html` | `/wallet/settings` | Auth |
| 10 | `change-password.html` | `/wallet/change-password` | Auth |
| 11 | `delete-account.html` | `/wallet/delete-account` | Auth |
| 12 | `daily-limit-edit.html` | `/wallet/daily-limit/edit` | Auth |
| 13 | `transaction-export.html` | `/wallet/export` | Auth |
| 14 | `contact-us.html` | `/wallet/contact-us` | Auth |
| 15 | `privacy-policy.html` | `/wallet/privacy` | Auth |
| 16 | `terms-of-service.html` | `/wallet/terms` | Auth |
| 17 | `wallet-send-message.html` | `/wallet/messages/send` | Auth |
| 18 | `wallet-messages.html` | `/wallet/messages` | Auth |
| 19 | `wallet-message-view.html` | `/wallet/messages/{id}` | Auth |
| 20 | `deposit.html` | `/transactions/deposit` | Auth |
| 21 | `withdraw.html` | `/transactions/withdraw` | Auth |
| 22 | `transfer.html` | `/transactions/transfer` | Auth |
| 23 | `transfer-confirm.html` | `/transactions/transfer/confirm` | Auth |
| 24 | `transaction-history.html` | `/transactions/history` | Auth |
| 25 | `profile.html` | `/profile` | Auth |
| 26 | `profile-edit.html` | `/profile/edit` | Auth |
| 27 | `admin.html` | `/admin` | Admin |
| 28 | `admin-login-activity.html` | `/admin/login-activity` | Admin |
| 29 | `admin-user-manage.html` | `/admin/users/{id}/manage` | Admin |
| 30 | `admin-send-message.html` | `/admin/messages/send` | Admin |
| 31 | `admin-message-inbox.html` | `/admin/messages/inbox` | Admin |
| 32 | `admin-message-thread.html` | `/admin/messages/users/{username}/thread` | Admin |
| 33 | `admin-risk-reviews.html` | `/admin/risk-reviews` | Admin |

**Non-page endpoints:** `GET /wallet/export/pdf` (PDF download), `POST` redirects for cancel/clear/approve/reject.

### Fragments (4)

| Fragment | File |
|----------|------|
| App footer (version) | `fragments/app-footer.html` |
| Password visibility toggle | `fragments/password-visibility-toggle.html` |
| Spending category field | `fragments/spending-category-field.html` |
| Transaction filter fields | `fragments/transaction-filter-fields.html` |

---

## Security

### Main application (required)

| Mechanism | Implementation |
|-----------|----------------|
| Authentication | Spring Security form login (`/spring-security-login`) |
| Password storage | BCrypt |
| Session | HTTP session + `JSESSIONID` |
| CSRF | Enabled on all POST forms |
| Roles | `ROLE_USER`, `ROLE_ADMIN` |
| Authorization | Public paths; `/admin/**` → ADMIN; else authenticated |
| Account lock | `INACTIVE` blocks login |
| Secrets | `MAIL_PASSWORD`, `ADMIN_PASSWORD` via env |

### Microservice (API key)

Service-to-service auth via shared secret (not Spring Security login):

| Side | Property | Default |
|------|----------|---------|
| Main app (Feign) | `app.risk-service.api-key` | `aswallet-dev-api-key` |
| Microservice | `app.security.api-key` | `aswallet-dev-api-key` |

Feign interceptor (`RiskServiceFeignConfig`) sends `X-API-Key` on every call. Override both with `$env:RISK_SERVICE_API_KEY`.

---

## Scheduling & Caching

| Job | Trigger | Purpose |
|-----|---------|---------|
| `PendingTransferScheduler.processPendingTransfers` | Cron `app.transfer.process.cron` | Complete `PENDING` transfers |
| `PendingTransferScheduler.cancelStalePendingTransfers` | Fixed delay | Cancel stale pending |
| `PasswordResetTokenCleanupScheduler` | Cron `app.password-reset.cleanup.cron` | Purge expired tokens |

| Cache | Purpose |
|-------|---------|
| `profiles` | Profile reads |
| `walletSettings` | Settings reads |
| `transactionHistory` | History reads |

Eviction via `ApplicationCacheEviction`.

---

## Email & Events

| Event | Listener | Emails |
|-------|----------|--------|
| `UserRegisteredEvent` | `UserRegisteredEventListener` | Registration confirmation |
| `TransactionCompletedEvent` | `TransactionCompletedEventListener` | Deposit / withdraw / transfer |

SMTP: `smtp.abv.bg:465` — requires `MAIL_PASSWORD`.

---

## Error Handling

`GlobalExceptionHandler` — built-in + **26 custom exceptions**, e.g.:

`InsufficientBalanceException`, `TransferBlockedByRiskException`, `RiskReviewServiceException`,
`DailyWithdrawLimitExceededException`, `InvalidCardDetailsException`, `PendingTransferNotFoundException`,
`CannotCancelTransferException`, `MailboxMessageNotFoundException`, `UserNotFoundException`, …

---

## Frontend Assets

### CSS - 16 files (`static/css/`)

Entry: `style.css` imports the rest in cascade order.

| File | Scope |
|------|-------|
| `style.css` | Entry point (imports below) |
| `base/variables.css` | Design tokens, colours, spacing |
| `base/reset.css` | Reset, typography, footer base |
| `base/layout-panels.css` | Panels, backgrounds, shared layout |
| `base/buttons.css` | Shared button / pill styles |
| `pages/home.css` | Landing, bank-card login/register CTAs |
| `pages/auth.css` | Auth forms, materialize reveal animations |
| `pages/wallet.css` | Dashboard, balance card, quick actions |
| `pages/transactions.css` | Deposit, withdraw, transfer, history, pagination |
| `pages/bank-details.css` | Bank card add/view, card preview |
| `pages/profile.css` | Profile view, daily limit |
| `pages/settings.css` | Settings hub, toggles |
| `pages/admin.css` | Admin panel, risk reviews, status chips, mailbox |
| `pages/account-security.css` | Change password, delete account |
| `pages/contact-us.css` | Contact page |
| `pages/legal-pages.css` | Privacy Policy, Terms of Service |

### JavaScript - 17 files (`static/js/`)

| File | Used on | Purpose |
|------|---------|---------|
| `home.js` | Most pages | Materialize reveal animations, bank-card shine on landing |
| `password-toggle.js` | Login, register, password forms | Show/hide password fields |
| `bank-details.js` | Bank details | Card number formatting, live card preview |
| `wallet-card.js` | Wallet dashboard | Balance card hover/animation |
| `wallet-iban.js` | Wallet dashboard | IBAN show/hide toggle |
| `wallet-settings.js` | Settings | AJAX save for toggles |
| `deposit.js` | Deposit | Deposit form helpers |
| `withdraw.js` | Withdraw | Amount step init (via shared module) |
| `transfer.js` | Transfer | Amount step init for transfer form |
| `transaction-amount-step.js` | Deposit, withdraw, transfer | Shared ±€5 amount stepper, min/max clamp |
| `transaction-history.js` | History (when pending) | Auto-refresh while pending transfers exist |
| `change-password.js` | Change password | Strength meter, requirement checklist |
| `daily-limit-edit.js` | Daily limit | Stepper UI for limit slider |
| `profile-edit.js` | Profile edit | Avatar image preview before upload |
| `admin.js` | Admin dashboard | Admin panel interactions |
| `risk-reviews.js` | Admin risk reviews | Master-detail list selection, keyboard nav |

### Images (`static/images/`)

Page backgrounds and assets referenced by templates (under `static/images/`):

| Path | Used by |
|------|---------|
| `homePage.png` | Home |
| `reg-log.png` | Login, register |
| `asset/aswallet-logo.png` | Login, register |
| `forgot-password.png` | Forgot password |
| `res-pass.png` | Reset password |
| `delete-account.png` | Delete account, account deleted |
| `walletLight.png` | Wallet, admin inbox |
| `bank-details-light.png` | Bank details |
| `settingsLight.png` | Settings, privacy, terms |
| `change-password-light.png` | Change password |
| `userDetails.png` | Profile, profile edit |
| `daily-limit.png` | Daily limit edit |
| `deposit.png` | Deposit, withdraw |
| `transfer.png` | Transfer, transfer confirm |
| `history.png` | Transaction history |
| `export-file.png` | Transaction export |
| `contactUs.png` | Contact us |
| `mailbox.png` | Mailbox pages |
| `5.png` | Admin pages |
| `asset/emv-chip.png` | Home, wallet, bank details cards |
| `asset/noContact.png` | Home, wallet, bank details cards |
| `default-avatar.svg` | Default profile avatar |

User-uploaded avatars: `uploads/avatars/` (gitignored).

---

## Backend Source Inventory

### Controllers (15)

| Controller | Responsibility |
|------------|----------------|
| `HomeController` | Landing page |
| `UserController` | Register, login, logout |
| `PasswordResetController` | Forgot / reset password |
| `WalletController` | Dashboard, bank card, settings, delete account |
| `TransactionController` | Deposit, withdraw, transfer, confirm |
| `TransactionHistoryController` | History, pagination, cancel, clear |
| `TransactionExportController` | Filter preview, PDF download |
| `ProfileController` | Profile view / edit |
| `DailyLimitController` | Daily withdraw limit editor |
| `ContactUsController` | Contact page |
| `LegalPagesController` | Privacy Policy, Terms of Service |
| `WalletMailboxController` | User mailbox |
| `AdminController` | Admin dashboard, users, roles, status |
| `AdminMailboxController` | Admin messaging |
| `AdminRiskReviewController` | Risk review approve/reject/clear |

### Service interfaces (16)

`AdminMailboxService`, `AdminDashboardService`, `AdminRiskReviewService`, `AdminService`, 
`BankCardService`, `EmailService`, `LoginActivityService`, `PasswordResetService`,
`TransactionExportService`, `TransactionService`, `TransferRiskAssessmentService`,
`UserProfileDetailsService`, `UserService`, `WalletService`, `WithdrawDailyLimitService`,
`AvatarStorageService`

### Service implementations (21)

`AdminMailboxServiceImpl`, `AdminDashboardServiceImpl` , `AdminRiskReviewServiceImpl`, `AdminServiceImpl`,
`ApplicationCacheEviction`, `AppUserDetailsService`, `AvatarStorageServiceImpl`,
`BankCardServiceImpl`, `EmailServiceImpl`, `LoginActivityServiceImpl`,
`PasswordResetServiceImpl`, `PendingTransferProcessingService`, `TransactionExportServiceImpl`,
`TransactionServiceImpl`, `TransferRefundSupport`, `TransferRiskAssessmentServiceImpl`,
`UserDeletionService`, `UserProfileDetailsServiceImpl`, `UserServiceImpl`,
`WalletServiceImpl`, `WithdrawDailyLimitServiceImpl`

### Repositories (8)

`AdminMailboxMessageRepository`, `BankCardRepository`, `LoginActivityRepository`,
`PasswordResetTokenRepository`, `TransactionRepository`, `UserProfileDetailsRepository`,
`UserRepository`, `WalletRepository`

### Configuration (11)

`AdminBootstrapConfig`, `AppModelAdvice`, `AsyncConfig`, `AvatarUploadConfig`,
`BankCardIbanBackfillConfig`, `CacheConfig`, `SchedulingConfig`, `SecurityConfig`,
`SupportMailboxBackfillConfig`, `WalletBalanceBackfillConfig`, `WithdrawDailyLimitBackfillConfig`

### Other packages

| Package | Files | Contents |
|---------|-------|----------|
| `Clients/` | 4 | `RiskAssessmentClient` + Feign DTOs |
| `DTOs/` | 27 | Request/response/view DTOs |
| `Enums/` | 8 | Domain enums |
| `Events/` | 4 | 2 events + 2 listeners |
| `Scheduling/` | 2 | Transfer + token cleanup schedulers |
| `Security/` | 2 | `AppUserPrincipal`, `ReloadUserAuthoritiesFilter` |
| `Specifications/` | 1 | `TransactionSpecifications` |
| `Utils/` | 5 | IBAN, card validation, IP, dates, patterns |
| `GlobalExceptionHandler/` | 1 | Central error handling |

### DTOs (28)

`AdminRecipientOptionDTO`, `AdminDashboardSummaryDTO` , `AdminRiskAssessmentViewDTO`, `AdminSendMessageRequest`,
`AdminUserViewDTO`, `BankCardRequest`, `BankCardViewDTO`, `ChangePasswordRequest`,
`DailyLimitEditRequest`, `DeleteAccountRequest`, `DepositMoneyDTO`, `ForgotPasswordRequest`,
`LoginActivityViewDTO`, `LoginDTO`, `MailboxMessageViewDTO`, `ProfileEditRequest`,
`ResetPasswordRequest`, `TransactionHistoryFilter`, `TransactionViewDTO`, `TransferMoneyDTO`,
`UserDTO`, `UserProfileDetailsViewDTO`, `UserSendMessageRequest`, `UserViewDTO`,
`WalletSettingsRequest`, `WalletViewDTO`, `WithdrawDailyLimitViewDTO`, `WithdrawMoneyDTO`

---

| Variable | Purpose |
|----------|---------|
| `MAIL_PASSWORD` | SMTP |
| `ADMIN_PASSWORD` | Bootstrap admin password |
| `ADMIN_CARD_*` | Optional admin bootstrap card |

Use `application-local.properties` (gitignored) for local DB credentials.

---

## Getting Started

**Prerequisites:** Java 21, Maven, MySQL.

```powershell
$env:MAIL_PASSWORD = "your-smtp-password"
mvn spring-boot:run
# → http://localhost:8080

$env:DB_PASSWORD = "your_mysql_password"
mvn spring-boot:run
# → http://localhost:8081
```

**LAN / phone testing:** `server.address=0.0.0.0` is set. On phone use `http://<PC-LAN-IP>:8080` and set `app.risk-service.base-url=http://<PC-LAN-IP>:8081`.

---

## Testing & Coverage

| Metric | Value                                                |
|--------|------------------------------------------------------|
| Test classes | **26**                                               |
| Test methods | **~272** (excl. optional `ASWalletApplicationTests`) |
| Line coverage | **77.8%** JaCoCo ✅                                   |

```powershell
mvn test "-Dtest=!ASWalletApplicationTests"
```

### Unit tests — Services (12 classes)

`TransactionServiceImplTest`, `UserServiceImplTest`, `WalletServiceImplTest`,
`BankCardServiceImplTest`, `PasswordResetServiceImplTest`, `PendingTransferProcessingServiceTest`,
`AdminRiskReviewServiceImplTest`, `AdminServiceImplTest`, `AdminMailboxServiceImplTest`, 
`UserProfileDetailsServiceImplTest`, `WithdrawDailyLimitServiceImplTest`, `AdminDashboardServiceImplTest`

### WebMvc tests — Controllers (13 classes)

`UserControllerWebMvcTest`, `PasswordResetControllerWebMvcTest`, `ProfileControllerWebMvcTest`,
`WalletControllerWebMvcTest`, `WalletMailboxControllerWebMvcTest`, `AdminControllerWebMvcTest`,
`AdminRiskReviewControllerWebMvcTest`, `AdminMailboxControllerWebMvcTest`,
`TransactionControllerDepositWebMvcTest`, `TransactionControllerWithdrawWebMvcTest`,
`TransactionControllerTransferWebMvcTest`, `LegalPagesControllerWebMvcTest`,
`TransactionHistoryControllerWebMvcTest`

### Other

`GlobalExceptionHandlerTest`, optional `ASWalletApplicationTests` (needs live MySQL)

---

## For Examiners / Reviewers

**5-min demo:** Register → card + bonus → deposit → transfer (trigger risk review) → history pagination → admin risk reviews → PDF export → privacy/terms.

**Test card:** `4111 1111 1111 1111` · future expiry · CVC `123`

**Admin:** `admin` / `admin123` (or `ADMIN_PASSWORD`)

| Issue | Fix |
|-------|-----|
| Risk review empty | Start svc on `:8081` |
| Transfer blocked | Receiver needs card, or score ≥ 70 |
| Phone can't connect | Use LAN IP for both apps |

---

## Version History

### v2.0.2 (current)

- Risk review **history** (`/manual-reviews`, status chips, Delete all)
- Transaction history **pagination**
- **Privacy Policy** + **Terms of Service**
- Feign: `listManualReviews`, `deleteManualReviews`, `decision` filter
- Tests: `LegalPagesControllerWebMvcTest`, `TransactionHistoryControllerWebMvcTest`
- `PENDING_RISK_REVIEW` end-to-end flow
- Admin risk UI, Feign PATCH/DELETE extensions

---

## Author

Developed by **AStoyanoff®**
