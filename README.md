<p align="center">
  <img
    src="https://readme-typing-svg.demolab.com?font=Orbitron&size=42&duration=2500&pause=1000&color=E53935&center=true&vCenter=true&width=700&lines=%F0%9F%92%B0+ASWallet-Vol.2+%F0%9F%92%B0"
    alt="ASWallet-Vol.2"
  />
</p>

---

<p align="center">
  <img
    src="src/main/resources/static/images/asset/screenShot.png"
    width="520"
    alt="ASWallet application preview"
  />
</p>

---

<p align="center">
  Full-Stack Personal FinTech Wallet Platform with an Integrated Risk Assessment Microservice
</p>

---

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Version-2.0.2-blue" alt="Version 2.0.2">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot" alt="Spring Boot 4.0.6">
  <img src="https://img.shields.io/badge/Spring%20Security-Authentication-success?logo=springsecurity" alt="Spring Security">
  <img src="https://img.shields.io/badge/MySQL-Database-blue?logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Thymeleaf-Frontend-green?logo=thymeleaf" alt="Thymeleaf">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven" alt="Maven">
  <img src="https://img.shields.io/badge/JaCoCo-77.8%25-success" alt="JaCoCo coverage">
</p>

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Tech Stack](#tech-stack)
4. [Domain Model](#domain-model)
5. [Application Features](#application-features)
6. [Risk Assessment Microservice](#risk-assessment-microservice)
7. [Web Pages and Routes](#web-pages-and-routes)
8. [Security](#security)
9. [Scheduling and Caching](#scheduling-and-caching)
10. [Email and Events](#email-and-events)
11. [Error Handling](#error-handling)
12. [Project Structure](#project-structure)
13. [Configuration](#configuration)
14. [Getting Started](#getting-started)
15. [Testing and Coverage](#testing-and-coverage)
16. [For Examiners and Reviewers](#for-examiners-and-reviewers)
17. [Version History](#version-history)
18. [Author](#author)

---

## Project Overview

**ASWallet-Vol.2** is a full-stack personal finance application built with Spring Boot and Thymeleaf.

It provides wallet management, deposits, withdrawals, transfers, transaction history, PDF export, user profiles, messaging, administration tools and security features.

Transfers are evaluated by an independent REST microservice:

- **Main application:** `ASWallet-Vol.2`
- **Risk microservice:** [`ASWallet-Vol.2-svc`](https://github.com/AStoyan0ff/ASWallet-Vol.2-svc)

The applications communicate through Spring Cloud OpenFeign and are protected with a shared API key.

### Project Statistics

| Area | Count |
|------|------:|
| Java source files | 163 |
| Controllers | 15 |
| Service interfaces | 17 |
| Service implementations | 22 |
| JPA repositories | 8 |
| JPA entities | 9 |
| DTOs | 29 |
| Enums | 8 |
| Custom exceptions | 26 |
| Configuration classes | 11 |
| Thymeleaf pages | 34 |
| Reusable fragments | 4 |
| CSS files | 16 |
| JavaScript files | 19 |
| Test classes | 26 |
| Test methods | ~272 |
| JaCoCo line coverage | **77.8%** ✅ |

---

## System Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Browser / Thymeleaf UI                    │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              ASWallet Main Application (:8080)              │
│                                                             │
│  Controllers → Services → Repositories → MySQL              │
│       │                                                     │
│       ├── Security, Cache, Scheduling and Events             │
│       ├── SMTP Email Service                                 │
│       └── OpenFeign RiskAssessmentClient                     │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST + X-API-Key
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             Risk Assessment Microservice (:8081)             │
│                                                             │
│  Controller → Services → Repositories → MySQL               │
└─────────────────────────────────────────────────────────────┘
```

### Application Layers

- **Presentation:** Spring MVC controllers, Thymeleaf templates and DTOs
- **Business:** Service interfaces and implementations
- **Persistence:** Spring Data JPA repositories and entities
- **Security:** Authentication, authorization, CSRF and API-key validation
- **Integration:** Spring Cloud OpenFeign
- **Cross-cutting:** Events, caching, scheduling and exception handling

### Applications

| Application | Port | Database | Responsibility |
|-------------|------|----------|----------------|
| `ASWallet-Vol.2` | `8080` | `as_wallet` | UI, wallet operations, security and Feign client |
| `ASWallet-Vol.2-svc` | `8081` | `as_wallet_svc` | Transfer risk assessment REST API |

---

## Tech Stack

| Area | Technology |
|------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring MVC |
| Frontend | Thymeleaf, HTML5, CSS3, vanilla JavaScript |
| Security | Spring Security, BCrypt, CSRF |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL |
| Validation | Jakarta Bean Validation |
| Integration | Spring Cloud OpenFeign 2025.1.1, Feign HC5 |
| Email | Spring Mail, SMTP |
| Events | Spring Application Events |
| Scheduling | Spring Scheduling |
| Caching | Spring Cache |
| PDF export | OpenPDF 2.0.3 |
| Testing | JUnit 5, Mockito, MockMvc, JaCoCo |
| Build | Maven |
| Utilities | Lombok |

---

## Domain Model

All entities use UUID primary keys inherited from `BaseClass`.

### Relationships

```text
User (1) ───── (1) Wallet
  │                │
  │                └──── (*) Transaction
  │                         sender / receiver
  │
  ├──── (1) UserProfileDetails
  ├──── (1) BankCard
  ├──── (*) LoginActivity
  └──── (*) PasswordResetToken

AdminMailboxMessage ───── User
```

### Entities

| Entity | Database table | Purpose |
|--------|----------------|---------|
| `User` | `users` | Account credentials and role |
| `Wallet` | `wallets` | User balance |
| `Transaction` | `transactions` | Deposits, withdrawals and transfers |
| `BankCard` | `bank_cards` | Card information and generated IBAN |
| `UserProfileDetails` | `user_profile_details` | Profile, status and wallet settings |
| `AdminMailboxMessage` | `admin_mailbox_messages` | Communication between users and administrators |
| `PasswordResetToken` | `password_reset_tokens` | Forgot-password tokens |
| `LoginActivity` | `login_activity` | Authentication audit records |
| `BaseClass` | — | UUID mapped superclass |

### Enums

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

### Authentication and Account Management

- User registration, login and logout
- BCrypt password hashing
- Forgot-password and reset-password flow
- SHA-256 hashed reset tokens
- Token expiration and scheduled cleanup
- Password change
- Account deletion
- Inactive account login protection
- Login activity audit

### Wallet and Transactions

- Automatic wallet creation after registration
- €50 welcome bonus after adding the first bank card
- Deposits and withdrawals
- Configurable daily withdrawal limit
- Two-step transfer confirmation
- Risk-aware transfers through the microservice
- Pending transfer processing
- Manual transfer cancellation
- Automatic cancellation of stale transfers
- Transaction categories
- Transaction history with filters and pagination
- PDF export using JPA Specifications

### Bank Card

- Add a card during registration or from the wallet
- Live card preview
- Masked card information
- Card validation
- Automatically generated IBAN

### Profile and Settings

- View and edit profile information
- Avatar upload and preview
- Hide or display wallet balance
- Daily withdrawal limit editor
- Contact Us page
- Privacy Policy
- Terms of Service

### Payments

The `/wallet/payments` page displays outgoing transfers with:

- Receiver avatar and username
- Transfer amount
- Transaction status
- Transaction date
- Five visible rows with in-panel scrolling

### Messaging

Users can:

- Send messages to the administrator
- Open their inbox
- View and delete messages
- See an unread-message badge

Administrators can:

- Send messages to users
- View the admin inbox
- Follow message threads
- Monitor unread messages

### Administration

- User management
- Account status management
- Role management
- User deletion
- Login activity audit
- Risk review history
- Approve or reject transfers
- Clear completed risk reviews
- Administrative mailbox

---

## Risk Assessment Microservice

The independent microservice is available in:

[`ASWallet-Vol.2-svc`](https://github.com/AStoyan0ff/ASWallet-Vol.2-svc)

The main application sends transfer information through `RiskAssessmentClient`.

### Risk Decisions

| Decision | Transaction status | Behaviour |
|----------|--------------------|-----------|
| `ALLOW` | `PENDING` | The scheduler completes the transfer |
| `REVIEW` | `PENDING_RISK_REVIEW` | Funds are held until an administrator reviews the transfer |
| `BLOCK` | Not created | The transfer is rejected immediately |

### Manual Review Endpoints

- `GET /manual-reviews` — return transfers requiring manual review
- `PATCH /manual-reviews/{id}` — approve or reject a review
- `DELETE /manual-reviews` — delete review history

### Fail-Open Behaviour

```properties
app.risk-service.fail-open=true
```

When enabled, transfers can continue if the risk microservice is temporarily unavailable.

---

## Web Pages and Routes

### Public Pages

| Template | Route |
|----------|-------|
| `home.html` | `/` |
| `login.html` | `/login` |
| `register.html` | `/register` |
| `forgot-password.html` | `/forgot-password` |
| `reset-password.html` | `/reset-password` |
| `account-deleted.html` | `/account-deleted` |

### Authenticated User Pages

| Template | Route |
|----------|-------|
| `wallet.html` | `/wallet` |
| `bank-details.html` | `/wallet/bank-details` |
| `settings.html` | `/wallet/settings` |
| `payments.html` | `/wallet/payments` |
| `change-password.html` | `/wallet/change-password` |
| `delete-account.html` | `/wallet/delete-account` |
| `daily-limit-edit.html` | `/wallet/daily-limit/edit` |
| `transaction-export.html` | `/wallet/export` |
| `contact-us.html` | `/wallet/contact-us` |
| `privacy-policy.html` | `/wallet/privacy` |
| `terms-of-service.html` | `/wallet/terms` |
| `wallet-send-message.html` | `/wallet/messages/send` |
| `wallet-messages.html` | `/wallet/messages` |
| `wallet-message-view.html` | `/wallet/messages/{id}` |
| `deposit.html` | `/transactions/deposit` |
| `withdraw.html` | `/transactions/withdraw` |
| `transfer.html` | `/transactions/transfer` |
| `transfer-confirm.html` | `/transactions/transfer/confirm` |
| `transaction-history.html` | `/transactions/history` |
| `profile.html` | `/profile` |
| `profile-edit.html` | `/profile/edit` |

### Administrator Pages

| Template | Route |
|----------|-------|
| `admin.html` | `/admin` |
| `admin-login-activity.html` | `/admin/login-activity` |
| `admin-user-manage.html` | `/admin/users/{id}/manage` |
| `admin-send-message.html` | `/admin/messages/send` |
| `admin-message-inbox.html` | `/admin/messages/inbox` |
| `admin-message-thread.html` | `/admin/messages/users/{username}/thread` |
| `admin-risk-reviews.html` | `/admin/risk-reviews` |

### Reusable Thymeleaf Fragments

- Application footer with the current version
- Password visibility toggle
- Spending category field
- Transaction filter fields

---

## Security

### Main Application

| Mechanism | Implementation |
|-----------|----------------|
| Authentication | Spring Security form login |
| Login processing | `/spring-security-login` |
| Password storage | BCrypt |
| Session management | HTTP session and `JSESSIONID` |
| CSRF protection | Enabled for state-changing forms |
| User roles | `ROLE_USER`, `ROLE_ADMIN` |
| Admin authorization | `/admin/**` requires `ROLE_ADMIN` |
| Account protection | Inactive accounts cannot log in |
| Secrets | Loaded from environment variables |

### Microservice API Key

Service-to-service communication is protected with the `X-API-Key` header.

| Application | Property |
|-------------|----------|
| Main application | `app.risk-service.api-key` |
| Risk microservice | `app.security.api-key` |

---

### Optional Environment Variables

| Variable | Purpose                                                                      |
|----------|------------------------------------------------------------------------------|
| `RISK_SERVICE_API_KEY` | **Overrides the default development API key used between both applications** |

---

## Scheduling and Caching

### Scheduled Jobs

| Job | Trigger | Purpose |
|-----|---------|---------|
| `processPendingTransfers` | Configurable cron | Complete pending transfers |
| `cancelStalePendingTransfers` | Fixed delay | Cancel expired pending transfers |
| `PasswordResetTokenCleanupScheduler` | Configurable cron | Delete expired reset tokens |

---

### Application Caches

| Cache | Purpose |
|-------|---------|
| `profiles` | User profile reads |
| `walletSettings` | Wallet settings reads |
| `transactionHistory` | Transaction history reads |

Cache eviction is managed through `ApplicationCacheEviction`.

---

## Email and Events

| Event | Listener | Purpose |
|-------|----------|---------|
| `UserRegisteredEvent` | `UserRegisteredEventListener` | Send registration confirmation |
| `TransactionCompletedEvent` | `TransactionCompletedEventListener` | Send transaction notification |

SMTP configuration:

```properties
spring.mail.host=smtp.abv.bg
spring.mail.port=465
```

The email password is loaded from `MAIL_PASSWORD`.

---

## Error Handling

`GlobalExceptionHandler` provides centralized handling for validation, upload and domain exceptions.

The application contains 26 custom exceptions, including:

- `InsufficientBalanceException`
- `TransferBlockedByRiskException`
- `RiskReviewServiceException`
- `DailyWithdrawLimitExceededException`
- `InvalidCardDetailsException`
- `PendingTransferNotFoundException`
- `CannotCancelTransferException`
- `MailboxMessageNotFoundException`
- `UserNotFoundException`

---

## Project Structure

```text
src
├── main
│   ├── java/STARTER
│   │   ├── Clients
│   │   ├── Configuration
│   │   ├── Controllers
│   │   ├── CustomException
│   │   ├── DTOs
│   │   ├── Enums
│   │   ├── Events
│   │   ├── GlobalExceptionHandler
│   │   ├── Models
│   │   ├── Repositories
│   │   ├── Scheduling
│   │   ├── Security
│   │   ├── Services
│   │   ├── Specifications
│   │   └── Utils
│   │
│   └── resources
│       ├── static
│       │   ├── css
│       │   ├── images
│       │   └── js
│       ├── templates
│       │   └── fragments
│       └── application.properties
│
└── test
    └── java/STARTER
```

### Frontend Organization

The frontend contains:

- 16 modular CSS files
- 19 JavaScript files
- 34 Thymeleaf pages
- 4 reusable Thymeleaf fragments
- Separate page backgrounds and application assets

User-uploaded avatars are stored in:

```text
uploads/avatars/
```

This directory is excluded from Git.

---

## Configuration

### Required Environment Variables

| Variable | Purpose |
|----------|---------|
| `DB_PASSWORD` | MySQL database password |
| `MAIL_PASSWORD` | SMTP account password |
| `ADMIN_PASSWORD` | Bootstrap administrator password |
| `RISK_SERVICE_API_KEY` | Shared microservice API key |
| `ADMIN_CARD_*` | Optional administrator card configuration |

Local credentials can be stored in:

```text
application-local.properties
```

This file should remain excluded from Git.

---

## Getting Started

### Prerequisites

- Java 21
- Maven
- MySQL
- Git

### 1. Clone the Main Application

```powershell
git clone https://github.com/AStoyan0ff/ASWallet-Vol.2.git
cd ASWallet-Vol.2
```

### 2. Clone the Risk Microservice

Clone it next to the main application:

```powershell
git clone https://github.com/AStoyan0ff/ASWallet-Vol.2-svc.git
```

Recommended directory structure:

```text
Projects/
├── ASWallet-Vol.2/
└── ASWallet-Vol.2-svc/
```

### 3. Create the Databases

```sql
CREATE DATABASE as_wallet;
CREATE DATABASE as_wallet_svc;
```

### 4. Set Environment Variables

```powershell
$env:DB_PASSWORD = "your-mysql-password"
$env:MAIL_PASSWORD = "your-smtp-password"
$env:ADMIN_PASSWORD = "your-admin-password"
$env:RISK_SERVICE_API_KEY = "your-secure-api-key"
```

### 5. Start the Risk Microservice

From `ASWallet-Vol.2-svc`:

```powershell
mvn spring-boot:run
```

The microservice starts at:

```text
http://localhost:8081
```

### 6. Start the Main Application

From `ASWallet-Vol.2`:

```powershell
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

### LAN and Phone Testing

The application can listen on all network interfaces:

```properties
server.address=0.0.0.0
```

Open the application from another device with:

```text
http://<PC-LAN-IP>:8080
```

Configure the risk service URL when testing across the local network:

```properties
app.risk-service.base-url=http://<PC-LAN-IP>:8081
```

---

## Testing and Coverage

| Metric | Result |
|--------|-------:|
| Test classes | 26 |
| Test methods | ~272 |
| JaCoCo line coverage | **77.8%** ✅ |
| Coverage target | 70% |

Run the test suite:

```powershell
mvn test
```

If the application context test requires a running MySQL instance, run:

```powershell
mvn test "-Dtest=!ASWalletApplicationTests"
```

### Test Categories

- Service unit tests with Mockito
- Spring MVC controller tests with MockMvc
- Security and authorization tests
- Risk-review workflow tests
- Transaction flow tests
- Global exception-handler tests
- Legal-page tests
- Transaction history and pagination tests

---

## For Examiners and Reviewers

### Suggested Five-Minute Demonstration

1. Register a new user
2. Add a bank card and receive the welcome bonus
3. Make a deposit
4. Create a transfer that triggers risk review
5. Open the paginated transaction history
6. Log in as administrator
7. Approve or reject the risk review
8. Export the transaction history as PDF
9. Open the Privacy Policy and Terms of Service

### Test Card

```text
Card number: 4111 1111 1111 1111
Expiration:  Any future date
CVC:         123
```

### Administrator

```text
Username: admin
Password: ADMIN_PASSWORD environment variable
```

### Troubleshooting

| Problem | Solution |
|---------|----------|
| Risk-review list is empty | Start the risk microservice on port `8081` |
| Transfer is blocked unexpectedly | Verify the receiver has a card and inspect the risk score |
| Microservice returns `401` | Ensure both applications use the same API key |
| Phone cannot connect | Use the computer's LAN IP instead of `localhost` |
| Email cannot be sent | Verify `MAIL_PASSWORD` and the SMTP configuration |
| Application cannot connect to MySQL | Verify the databases and `DB_PASSWORD` |

---

## Version History

### v2.0.2 — Current

- Added risk-review history and status indicators
- Added manual review approval and rejection
- Added deletion of risk-review history
- Added `PENDING_RISK_REVIEW` transfer flow
- Added transaction history pagination
- Added Privacy Policy and Terms of Service
- Added the Payments page
- Added outgoing transfer information
- Extended the Feign client with PATCH and DELETE operations
- Added controller tests for legal pages and transaction history
- Improved the wallet balance card and IBAN visibility control
- Improved the quick-action button design

---

## Author

Developed by **Andrey Stoyanov**

GitHub: [AStoyan0ff](https://github.com/AStoyan0ff)

<p align="center">
  <strong>ASWallet-Vol.2 — Your money, your control.</strong>
</p>