# MotorPH Payroll Management System

MotorPH is a JavaFX payroll and HR application developed by **Group 2 – H4101** for Information Assurance and Security.

The system supports employee records, attendance, leave and overtime requests, payroll processing, payslips, reports, support tickets, role-based access, and audit logs.

## Tech Stack

- Java 21 and JavaFX
- Spring Boot REST API
- MySQL 8
- Spring Security and JWT
- TOTP authenticator verification
- JasperReports
- Maven and Docker Compose

The JavaFX desktop communicates with the backend REST API. Only the backend connects directly to MySQL.

## Requirements

- Git
- JDK 21
- Maven 3.9 or later
- Docker Desktop
- Google Authenticator or Microsoft Authenticator

## How to Run

### 1. Clone the repository

```bash
git clone https://github.com/MsYelkyy/IAS2-MotorPH-Payroll-System.git
cd IAS2-MotorPH-Payroll-System
```

### 2. Configure the environment

Copy `.env.example` to `.env`.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Git Bash:

```bash
cp .env.example .env
```

Generate the required secrets:

```bash
openssl rand -base64 48
openssl rand -base64 32
```

Place the first value in `MOTORPH_JWT_SECRET` and the second value in `MOTORPH_TOTP_ENCRYPTION_KEY` inside `.env`.

Do not commit or share the `.env` file.

### 3. Start the backend and database

Make sure Docker Desktop is running, then execute:

```bash
docker compose up -d --build
```

Check that the services are running:

```bash
docker compose ps
```

### 4. Start the desktop application

```bash
mvn -pl motorph-desktop javafx:run
```

## Demo Account

Use the following local demo account:

| Field | Value |
|---|---|
| User | Manuel Garcia |
| Company email | `manuel.garcia@motorph.com` |
| Password | `MotorPH@10001` |

On the first sign-in, scan the displayed QR code using Google Authenticator or Microsoft Authenticator. Enter the current six-digit code to complete TOTP setup.

Future sign-ins require the company email, password, and current authenticator code.

## Implemented Security Controls

### Control 1: Secure Authentication and Password Protection

- PBKDF2-HMAC-SHA256 password hashing with unique salts
- Stronger versioned password hashes with automatic legacy-hash upgrade
- TOTP setup, sign-in, and password recovery
- Automatic account lockout after repeated failed passwords
- Login rate limiting by account and client address
- JWT access and refresh token validation
- Session revocation after logout, password reset, account lock, or account disablement

### Control 2: Role-Based Access Control

- Backend-enforced roles and database permissions
- Active-role validation for protected operations
- Locked and inactive account checks on authenticated requests
- Permission and team restrictions for support tickets
- Payslip ownership protection
- Admin, Finance, and HR access to other employees’ payslips
- Controlled 401 and 403 responses

### Control 3: Secure Coding and Input Validation

- Jakarta Bean Validation for REST requests
- Parameterized SQL queries
- Controlled 400, 404, 405, and 415 responses
- JSON-only API request handling
- Profile-photo signature, extension, and size validation
- Sensitive passwords and tokens masked from logs
- Dependency and static-security scanning

### Control 4: Payroll Integrity

- Backend duplicate-payroll checks
- Composite unique constraint on employee and payrun
- Transactional payroll, employee, salary, and benefit updates
- Controlled duplicate-payroll conflict responses
- Protection against partial saves and concurrent duplicates

### Control 5: Audit Logging and Monitoring

- Centralized logging of authentication and sensitive actions
- Database-generated timestamps
- IP address and device information recording
- Restricted audit-log modification and deletion
- Logging for denied access, lockout, TOTP, configuration, ticket, attendance, and profile-photo events

## Tests

Run all automated tests:

```bash
mvn clean test
```

The project also uses manual JavaFX testing, Postman API testing, MySQL inspection, OWASP Dependency-Check, Semgrep, CodeQL, SonarQube, and Aikido Security.

## Stop the Application

Stop the services:

```bash
docker compose stop
```

Remove the containers while keeping the database:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the local database.

## Project Information

**Course:** Information Assurance and Security  
**Section:** H4101  
**Group:** Group 2  

### Group Members

- Joyce Mariel Poblete
- Angel Faye Nogal
- Alexaire Raymundo
- Mariah Angela Alvarez
