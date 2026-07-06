<h1 align="center">
  💰 ASWallet 💰
</h1>

<p align="center">
  <img src="https://spring.io/img/projects/spring-boot.svg" width="120">
</p>

<p align="center">
  Personal FinTech Wallet Application built with Spring Boot
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk">
  <img src="https://img.shields.io/badge/version-2.0.1-blue">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen?logo=springboot">
  <img src="https://img.shields.io/badge/Spring%20Security-Authentication-success?logo=springsecurity">
  <img src="https://img.shields.io/badge/MySQL-Database-blue?logo=mysql">
  <img src="https://img.shields.io/badge/Thymeleaf-Frontend-green?logo=thymeleaf">
  <img src="https://img.shields.io/badge/HTML5-Markup-E34F26?logo=html5">
  <img src="https://img.shields.io/badge/CSS3-Styling-1572B6?logo=css3">
  <img src="https://img.shields.io/badge/JavaScript-ES6-F7DF1E?logo=javascript&logoColor=black">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven">
</p>

<h1 align="center">
  AStoyanoff's Wallet Project
</h1>
<p align="center">
  ASWallet is a personal fintech/wallet web application built with Spring Boot.
  Users register an account, link a bank card, receive a welcome bonus, and manage wallet balance through deposits, withdrawals, and transfers in a secure environment.
</p>

---

## Recommended Display Settings (optional)

The UI was primarily tuned and reviewed at:

- Windows Display Scale: **125%**
- Browser Zoom: **75%–80%**
- Resolution: **1920×1080**

The layout is responsive (fluid panels, media queries). At **100% browser zoom**
the UI may appear slightly larger but should remain usable. These settings are
a reviewer convenience, not a requirement.

---

## Features

### Authentication & Security

* User Registration with Bean Validation (username, email, password)
* User Login & Logout
* Spring Security Authentication
* BCrypt Password Encryption
* Change Password Functionality
* Forgot Password (token-based reset via email)
* Custom UserDetailsService with role-based authorities (`USER` / `ADMIN`)
* Delete Account (reset tokens, bank card, transactions, wallet, user)

> Spring Advanced additions (profile, account status, transfer confirm, etc.) are documented in the **[Spring Advanced](#spring-advanced)** section below.

### Bank Card

* **No card required at registration** — register with account details only
* Add bank card later from **Bank Details** (`/wallet/bank-details`)
* **€50.00 welcome bonus** credited to the wallet on **first** successful card registration
* Welcome bonus recorded in transaction history (`Welcome bonus - bank card registered`)
* Only last 4 digits and card metadata stored (full PAN and CVC are not persisted)
* Deposit and withdraw require a registered bank card
* Transfer requires the **receiver** to have a registered bank card

### Wallet Management

* Automatic wallet creation after registration (starting balance **€0.00**)
* **€50.00 welcome bonus** when the user adds their first bank card
* Wallet balance tracking
* Deposit funds (from saved card + CVC confirmation)
* Withdraw funds (to saved card)
* Transfer funds between users (by receiver username)
* **Transfer confirm** — two-step review/confirm flow (see [Spring Advanced](#spring-advanced))
* Transaction history with type, status, amount, description, date, from/to
* Transaction statuses: `COMPLETED` (default for successful operations), `PENDING`, `FAILED`, `CANCELLED` — color-coded chips in history
* Successful wallet operations are saved as `COMPLETED` (`TransactionServiceImpl` + `@PrePersist` default)
* Transaction validation (amount limits, balance checks, description patterns)

### Admin Panel

* Role-based access (`/admin/**` - `ROLE_ADMIN` only)
* View all registered users with wallet balance and currency
* Remove users (transactions, wallet, bank card, reset tokens, and account) via `UserDeletionService`
* Default admin account and bank card bootstrapped on first startup
* Admin Panel button on `/wallet` (visible for admin only)

### Email Notifications

* Registration confirmation email
* Password reset link email
* Transaction notification emails (deposit, withdraw, transfer sent/received, welcome bonus)

Email sending uses **Spring Mail (SMTP)** and is decoupled from business logic using **Spring Application Events**:

* `UserRegisteredEvent` → registration email (after commit)
* `TransactionCompletedEvent` → transaction emails (after commit)

### UI

* Dark theme with page-specific background images
* Server-rendered Thymeleaf templates
* Glass-style form panels (`backdrop-filter`, semi-transparent overlays)
* **Materialize effect** on form panels — blur reveal, staggered fields, animated neon border, ambient glow pulse
* **Danger styling** — dark red neon outline and glow on Delete Account and Forgot Password
* **Password visibility toggle** (eye icon) on login, register, reset/change password, delete account, bank details CVC
* Responsive layout; some forms aligned left/right on desktop, centered on mobile
* **App version footer** on all pages (`ASWallet v2.0.1`) via shared Thymeleaf fragment
* Client-side helpers: card number formatting (`bank-details.js`), password strength (`change-password.js`), scroll/materialize animations (`home.js`), password toggle (`password-toggle.js`)

### Error Handling

* Global Exception Handling
* Custom Exceptions

Examples:

* `UserNotFoundException`
* `UserAlreadyExistsException`
* `PasswordMismatchException`
* `WalletNotFoundException`
* `NotTransferMoneyYourselfException`
* `ReceiverNotFoundException`
* `ReceiverBankCardNotFoundException`
* `SenderNotFoundException`
* `InsufficientBalanceException`
* `EmailAlreadyExistsException`
* `InvalidOrExpiredTokenException`
* `InvalidCardDetailsException`
* `CannotDeleteSelfException`
* `CannotDeleteAdminException`

---

## Spring Advanced

Work added for the **Spring Advanced** 

Advanced-related code is marked in the codebase with `// Advanced`, `<!-- Advanced -->`, or `/* Advanced */` comments.

### Implemented so far

| Area | Status | Notes |
|------|--------|-------|
| View & edit own profile | Done | `/profile`, `/profile/edit` |
| `UserProfileDetails` entity | Done | Separate table, one-to-one with `User` |
| Account status (`ACTIVE` / `INACTIVE`) | Done | Enum + login enforcement |
| Admin changes user status | Done | `/admin` — cannot change self or other admins |
| Transfer confirm (2-step) | Done | Review → confirm before execute |
| Account status in transaction history | Done | **Account Status** column (From/To users) |
| REST microservice + Feign | Pending | Required by assignment |
| 70% test coverage | Pending | Required by assignment |
| Scheduling & caching | Pending | Required by assignment |
| Admin role management | Pending | Required by assignment |

### User profile

Profile **`UserProfileDetails`** entity.

| Layer | Files |
|-------|-------|
| Entity | `Models/UserProfileDetails.java` |
| Repository | `Repositories/UserProfileDetailsRepository.java` |
| Service | `Services/Interface/UserProfileDetailsService.java`, `UserProfileDetailsServiceImpl.java` |
| DTOs | `UserProfileDetailsViewDTO.java`, `ProfileEditRequest.java` |
| Controller | `Controllers/ProfileController.java` |
| Templates | `profile.html`, `profile-edit.html` |

**Endpoints**

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/profile` | View profile (read-only) |
| GET | `/profile/edit` | Edit form |
| POST | `/profile/edit` | Save profile changes |

**Profile fields**

* First name, last name, phone, avatar URL — optional, editable by the user
* Username, email — read-only on edit form
* Avatar — circle image on view page; URL set only in edit form

**Validation** (`ProfileEditRequest` + `ValidationPatterns`)

* `OPTIONAL_PERSON_NAME` — first/last name
* `OPTIONAL_PHONE` — phone number
* `OPTIONAL_AVATAR_URL` — http(s) URL or `/images/...` path

**Lifecycle**

* Default profile created on **registration** (`UserServiceImpl` → `createDefaultForUser`)
* Missing profiles **backfilled on startup** (`AdminBootstrapConfig` → `ensureProfileExistsForAllUsers`)
* Profile deleted with user in `UserDeletionService`

### Account status

| Item | Detail |
|------|--------|
| Enum | `Enums/AccountStatus.java` — `ACTIVE`, `INACTIVE` |
| Storage | `UserProfileDetails.accountStatus` (default `ACTIVE`) |
| Profile view | User sees **Active** / **Inactive** on `/profile` (read-only) |
| Login | `AppUserPrincipal.isEnabled()` returns `true` only when status is `ACTIVE` |
| Inactive login | `UserController` catches `DisabledException` with a clear message |
| Admin UI | Status column + dropdown on `/admin` |
| Admin action | `POST /admin/users/{id}/status` with `ACTIVE` or `INACTIVE` |
| Restrictions | Admin cannot change **own** status or **other admin** accounts |
| Exceptions | `CannotChangeSelfAccountStatusException`, `CannotChangeAdminAccountStatusException` |

### Transfer confirm (2-step flow)

| Step | Endpoint | Action |
|------|----------|--------|
| 1 | `POST /transactions/transfer` | Validate form → store pending transfer in session → redirect |
| 2 | `GET /transactions/transfer/confirm` | Review page (`transfer-confirm.html`) |
| 3 | `POST /transactions/transfer/confirm` | Execute transfer → clear session |

Session key: `pendingTransferMoneyDTO`. User can go back to edit before confirming.

### Transaction history — account status

On `/transactions/history`, the table includes an **Account Status**:

* **Account Status** — shows `ACTIVE` / `INACTIVE` badge for each visible participant 

Helps users see whether transfer partners are inactive before sending money again.

Data is loaded in `TransactionServiceImpl` via `senderAccountStatus` / `receiverAccountStatus` on `TransactionViewDTO`.

### Admin panel (account status UI)

* Per-user status dropdown with **Update** (green) and **Remove** (red) buttons — equal size
* `AdminUserViewDTO.accountStatus` populated from `UserProfileDetailsService`

### UI pages (Advanced)

| Page | Template | Background |
|------|----------|------------|
| Profile view | `profile.html` | `userDetails.png` |
| Profile edit | `profile-edit.html` | `editDetails.png` |
| Transfer confirm | `transfer-confirm.html` | `transfer.png` |
| Admin (status) | `admin.html` | `5.png` |
| History (status) | `transaction-history.html` | `4.png` |

Wallet dashboard links: **Profile** → `/profile`, **History** → `/transactions/history`.

### Requirement mapping 

| Requirement | Covered by |
|-------------|------------|
| Authenticated users view/edit own profile | `/profile`, `/profile/edit` |
| Separate domain entity (`UserProfileDetails`) | Profile + account status |
| Thymeleaf + validation + error handling | Profile forms, `GlobalExceptionHandler` |
| Admin manages users | Delete user + change account status |
| CSRF on forms | All POST forms include `_csrf` token |

> **Note:** Profile update and account-status change operate on user-related data and may **not count** toward the required number of *valid domain functionalities* per the assignment rules. Wallet operations (deposit, withdraw, transfer) remain the primary domain functionalities.

### Still to do for assignment

* Second Spring Boot app (REST microservice) + **Feign Client**
* Separate databases for main app and microservice
* **70%** automated test coverage
* **Scheduling** (cron + non-cron) and **caching**
* Admin **role management** for other users
* Optional: Spring Boot **4.x** alignment per latest assignment text

---

## User Flow

1. **Register** at `/register` (username, email, password) → wallet + default profile created with **€0.00**
2. **Login** at `/login` (inactive accounts are rejected)
3. Open **Profile** from the wallet dashboard to view or edit personal details
4. Open **Bank Details** from the wallet dashboard
5. **Add bank card** → receive **€50.00 welcome bonus**
6. Use **Deposit**, **Withdraw**, **Transfer** (review → confirm), and **History** as needed

---

## Background Images

| Image | Page |
|-------|------|
| `2.png` | Home `/` |
| `log-reg.png` | Login, Register, Delete Account |
| `res-pass.png` | Forgot Password, Reset Password, Account Deleted |
| `wallet.png` | Wallet dashboard `/wallet` |
| `userDetails.png` | Profile `/profile` |
| `editDetails.png` | Edit Profile `/profile/edit` |
| `change-password.png` | Change Password |
| `6.png` | Bank Details |
| `4.png` | Deposit, Withdraw, Transaction History |
| `transfer.png` | Transfer |
| `5.png` | Admin Panel |

---

## Project Structure

```
src/main/java/STARTER
├── AsWalletApplication.java
├── Configuration
│   ├── SecurityConfig.java
│   ├── AdminBootstrapConfig.java
│   ├── AppModelAdvice.java
│   └── WalletBalanceBackfillConfig.java
├── Controllers
│   ├── HomeController.java
│   ├── UserController.java
│   ├── WalletController.java          
│   ├── TransactionController.java
│   ├── TransactionHistoryController.java
│   ├── ProfileController.java
│   ├── PasswordResetController.java
│   └── AdminController.java
├── Services
│   ├── Interface
│   │   └── UserProfileDetailsService.java
│   └── Impl
│       ├── UserDeletionService.java
│       ├── UserProfileDetailsServiceImpl.java
│       └── ...
├── Repositories
│   └── UserProfileDetailsRepository.java
├── Models
│   ├── User.java, Wallet.java, Transaction.java
│   ├── BankCard.java
│   └── UserProfileDetails.java
├── DTOs
│   ├── UserProfileDetailsViewDTO.java
│   └── ProfileEditRequest.java
├── Events
│   ├── UserRegisteredEvent.java
│   ├── UserRegisteredEventListener.java
│   ├── TransactionCompletedEvent.java
│   └── TransactionCompletedEventListener.java
├── Enums
│   ├── UserRole.java
│   ├── AccountStatus.java
│   ├── TransactionStatus.java
│   └── TransactionType.java
├── CustomException
├── GlobalExceptionHandler
└── Utils
    ├── DateTimeDisplay.java
    ├── ValidationPatterns.java
    └── CardValidationUtils.java

src/main/resources
├── templates
│   ├── home.html
│   ├── login.html, register.html
│   ├── forgot-password.html, reset-password.html
│   ├── wallet.html, bank-details.html
│   ├── deposit.html, withdraw.html, transfer.html, transfer-confirm.html
│   ├── transaction-history.html
│   ├── profile.html, profile-edit.html
│   ├── change-password.html, delete-account.html, account-deleted.html
│   ├── admin.html
│   └── fragments/app-footer.html, password-visibility-toggle.html
├── static
│   ├── css/style.css
│   ├── js/home.js, bank-details.js, change-password.js, deposit.js, password-toggle.js
│   └── images/
└── application.properties

src/test/java/STARTER
└── AsWalletApplicationTests.java
```

---

## Security

Passwords are never stored in plain text.

ASWallet uses:

* BCryptPasswordEncoder
* Spring Security Authentication
* Session-based Authentication
* Pending transfer data in (HTTP session) between review and confirm steps
* **CSRF protection** on all POST forms (hidden `_csrf` token)
* Role-based authorization (`ROLE_USER`, `ROLE_ADMIN`)
* SHA-256 hashed password reset tokens (single-use, expiring)
* Bank card data minimization (last 4 digits + holder name + expiry only)
* Sensitive credentials via environment variables where possible

---

## Configuration

### Database

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/as_wallet
spring.datasource.username = your-mysql-username
spring.datasource.password = your-mysql-password
```

### Environment Variables

| Variable | Purpose |
|----------|---------|
| `MAIL_PASSWORD` | SMTP password for `aswallet.noreply@abv.bg` |
| `ADMIN_PASSWORD` | Password for the default admin account (on first create) |

Optional admin bank card defaults (in `application.properties`):

| Property | Default |
|----------|---------|
| `app.admin.card.number` | `4111111111111111` |
| `app.admin.card.holder` | `AS Wallet Admin` |
| `app.admin.card.expiry-month` | `12` |
| `app.admin.card.expiry-year` | `30` |
| `app.admin.card.cvc` | `123` |

### Application Properties

```properties
app.version = 2.0.1
app.base-url = http://localhost:8080
app.password-reset.expiry-hours = 1
app.admin.username = admin
app.admin.email = aswallet.noreply@abv.bg
app.admin.password = ${ADMIN_PASSWORD:admin123}
spring.mail.host = smtp.abv.bg
spring.mail.port = 465
spring.mail.username = aswallet.noreply@abv.bg
spring.mail.password = ${MAIL_PASSWORD}
spring.thymeleaf.cache = false
spring.web.resources.cache.period = 0
```

Optional local overrides: create `application-local.properties` (gitignored) and uncomment `spring.config.import` in `application.properties` if needed.

---

## Getting Started

### Clone Repository

```bash
git clone https://github.com/AStoyan0ff/ASWallet.git
```

Application will start on:

```text
http://localhost:8080
```

---

## For Examiners / Reviewers

Quick checklist to run and test the project locally.

### Prerequisites

* **Java 21**
* **Maven**
* **MySQL** running on `localhost:3306`
* **`MAIL_PASSWORD`** - provided separately by the author (not in GitHub)

### Step 1 — Clone

```bash
git clone https://github.com/AStoyan0ff/ASWallet.git
```

### Step 2 — Database

Make sure MySQL is running and update credentials in `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url = jdbc:mysql://localhost:3306/as_wallet?createDatabaseIfNotExist=true
spring.datasource.username = your-mysql-username
spring.datasource.password = your-mysql-password
```

The database `as_wallet` is created automatically on first startup (`ddl-auto=update`).

### Step 3 - Environment variables

**PowerShell (before start):**

```powershell
$env:MAIL_PASSWORD = "smtp-password-from-author"
mvn spring-boot:run
```

**IntelliJ IDEA:** Run → Edit Configurations → Environment variables → `MAIL_PASSWORD=...`

| Variable | Required for startup? | Required for emails? |
|----------|----------------------|----------------------|
| `MAIL_PASSWORD` | No | **Yes** |
| `ADMIN_PASSWORD` | No (defaults to `admin123`) | No |

Without `MAIL_PASSWORD` the app **still starts**, but registration / reset / transaction emails will **not** be sent.

### Step 4 - Open the app

```text
http://localhost:8080
```

### Step 5 - Test the main flow

1. **Register** a new user at `/register` (no bank card on this step)
2. **Login** and open `/wallet` — balance should be **€0.00**
3. Open **Bank Details** and add a bank card (test number: `4111 1111 1111 1111`, any future expiry, CVC `123`)
4. Confirm **€50.00 welcome bonus** on wallet and in **Transaction History** (status **COMPLETED**)
5. Test **Deposit** (amount + CVC), **Withdraw**, and **Transfer** (Review Transfer → Confirm Transfer) to another user who also has a card
6. Optional: check inbox for registration and transaction emails

### Step 6 — Test email notifications

1. Register with a **real email address** you can open
2. Check inbox for **ASWallet - Successful registration**
3. Add bank card → welcome bonus email (if mail is configured)
4. Optional: deposit / withdraw / transfer — each action sends a notification email
5. Optional: **Forgot password** — check for reset link email

### Step 7 — Test Spring Advanced features (optional)

1. **Profile** — `/profile` (view), `/profile/edit` (update name, phone, avatar URL)
2. **Admin** — login as `admin`, open `/admin`, set a test user to **Inactive**, confirm login is blocked
3. **History** — `/transactions/history`, check **Account Status** column for From/To users
4. **Transfer confirm** — `/transactions/transfer` → Review Transfer → confirm on `/transactions/transfer/confirm`

### Default admin login

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` (unless `ADMIN_PASSWORD` is set) |

After login, open **Admin Panel** from the wallet page (`/admin`). Admin can view users, change **account status** (Active/Inactive), or remove non-admin users. Admin receives a default bank card on bootstrap and the welcome bonus when that card is first saved.

### Troubleshooting

| Problem | Likely cause |
|---------|----------------|
| App does not start | MySQL not running or wrong DB password in `application.properties` |
| No emails received | `MAIL_PASSWORD` not set or invalid |
| SMTP error on send | Wrong ABV mail password or SMTP blocked |
| Deposit / Withdraw blocked | No bank card — add one in Bank Details |
| Transfer fails | Receiver has no registered bank card |
| Login says account inactive | Admin set user status to `INACTIVE` — reactivate from Admin Panel |

For email testing, contact the author for the current `MAIL_PASSWORD`.

---

## Default Administrator Account

Created automatically on first startup if missing:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Email | `aswallet.noreply@abv.bg` |
| Password | `admin123` (or value of `ADMIN_PASSWORD` if set) |

Only the `admin` username has `ADMIN` role. All other registered users receive `USER` role.

---

## Version

Current release: **2.0.1** (`pom.xml` + `app.version` in `application.properties`).

The version is shown in a fixed footer on every page (`fragments/app-footer.html`, injected via `AppModelAdvice`).

---

## Future Improvements

* Edit / replace bank card after registration
* OTP / 2FA
* Transaction filters and export (PDF/CSV)
* **Desktop dashboard redesign** (deferred) — see [docs/planned/desktop-dashboard-variant.md](docs/planned/desktop-dashboard-variant.md)

---

## Learning Goals

This project was created to practice:

* Spring Boot
* Spring Security
* JPA & Hibernate
* Database Design
* MVC Architecture
* Clean Code Principles
* Exception Handling
* Financial Transaction Logic
* Spring Application Events
* Role-based Authorization
* Form validation and secure handling of payment-related data

## Author

Developed by AStoyanoff®
