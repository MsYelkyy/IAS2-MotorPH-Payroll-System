# MotorPH Payroll Management System

MotorPH Payroll Management System is a Java 21 payroll and HR application built
for the Advanced Object-Oriented Programming (AOOP) subject. The project is a
JavaFX desktop client talking to a Spring Boot REST backend over HTTP, with a
MySQL 8 database and a database reporting CI pipeline.

The system supports employee management, attendance, leave and overtime requests,
payroll processing, payslip viewing, role-based access control, support tickets,
audit logs, database tools, and PDF report generation.

---

## Current Architecture

The desktop client never touches MySQL. It has no database driver and no
database credentials; every read and write goes through the backend, which is
where all authentication, permission, validation, and audit logic runs. A
modified client cannot bypass those checks because they are not in the client.

```text
JavaFX Desktop Client  ->  HTTP/REST  ->  Spring Boot Backend  ->  JDBC  ->  MySQL
```

The repository is a Maven multi-module build with two peer applications:

```text
DPA-MotorPH/
|-- pom.xml                      # Aggregator only (packaging=pom), builds both modules
|
|-- motorph-desktop/             # JavaFX desktop client
|   |-- pom.xml
|   |-- src/main/java/motorph/
|   |   |-- api/                 # REST clients + wire DTOs (the only way out of the app)
|   |   |-- app/                 # JavaFX application entry point
|   |   |-- application/         # UI-side DTOs, exceptions, session, email preview
|   |   |-- domain/              # Value objects and models shared with the UI layer
|   |   |-- infrastructure/      # Email preview config only
|   |   `-- ui/                  # JavaFX controllers and UI helpers
|   `-- src/main/resources/      # FXML, CSS, images, fonts
|
|-- motorph-backend/             # Spring Boot REST API
|   |-- pom.xml
|   |-- src/main/java/motorph/backend/
|   |   |-- controller/          # REST controllers under /api/v1
|   |   |-- domain/              # Backend domain model
|   |   |-- dto/                 # Request/response DTOs
|   |   |-- exception/           # Backend exceptions
|   |   |-- reporting/           # Server-side JasperReports support
|   |   |-- repository/          # JDBC repositories
|   |   |-- security/            # Spring Security, JWT, password handling
|   |   `-- service/             # Backend business services
|   |-- src/test/java/           # Service and permission-gate tests
|   `-- src/main/resources/
|       |-- application.yml      # Backend runtime configuration
|       `-- motorph/             # Backend report templates and images
|
|-- database/
|   |-- payrollsystem_db.sql     # Main MySQL schema and seed data dump
|   |-- ci-cd/                   # GitHub Actions database pipeline entry scripts
|   `-- sql_scripts/             # Reporting views, seed script, and SQL tests
|
`-- .github/workflows/
    `-- database-report-ci.yml   # Database Report CI/CD Pipeline
```

The root `pom.xml` aggregates the two modules; it is not a parent they inherit
from. `motorph-backend` keeps `spring-boot-starter-parent` as its real parent,
and `motorph-desktop` stays a standalone POM.

### Security Model

- **No client-side authorization.** The desktop's role state is a display cache.
  Every endpoint re-checks the caller's permissions against the database.
- **Roles are never embedded in the JWT.** They are loaded per request, so a role
  change takes effect on the user's very next call rather than when a token expires.
- **Tokens are revocable.** Logout writes to a `token_revocation` table that the
  auth filter checks on every request, so a copied token dies at logout.
- **Audit identity is server-derived.** No endpoint accepts a user ID, role, or
  "acting as" field from a request body for auditing purposes.
- **Unauthenticated endpoints do not leak account existence.** `forgot-password`
  answers identically whether or not the address maps to an eligible account.

---

## Tech Stack

| Layer / Concern | Technology |
| --- | --- |
| Language | Java 21 |
| Desktop UI | JavaFX 21, FXML, CSS |
| Desktop API Client | Java `HttpClient`, Jackson |
| Backend API | Spring Boot 3.5.16, Spring MVC |
| Backend Security | Spring Security, JWT (`jjwt`), PBKDF2 password hashing |
| Validation | Spring Boot Validation / Jakarta Validation |
| Database | MySQL 8 (backend only) |
| Data Access | JDBC, Spring JDBC, HikariCP (backend only) |
| Reporting | SQL views, JasperReports 7.0.6, PDF export |
| Build Tool | Maven (multi-module) |
| Testing | JUnit 5, Mockito, AssertJ, Spring Boot Test |
| CI | GitHub Actions with MySQL 8 service container |

---

## Main Features

- Role-based login for Admin, HR, Finance, IT, and Employee users
- Two-factor authentication and JWT-backed API sessions
- Employee profile, salary, benefits, and government details management
- Employee self-service: contact details, profile photo, and password changes
- Attendance time-in/time-out and attendance history
- Leave and overtime submission, approval, rejection, and cancellation
- Payroll processing, payslip viewing, and PDF payslips
- PDF reports through JasperReports, generated server-side
- Support ticket routing and resolution
- System configuration for government rates and tax brackets
- User account administration, role permissions, and user access logs
- Password reset requests with IT resolution
- Database backup, download, restore, and delete
- Database report validation through GitHub Actions

---

## User Roles

| Role | Main Access |
| --- | --- |
| Admin | Full system access, company settings, reports, payroll, employees, leave records |
| IT | User accounts, roles, permissions, access logs, database tools, system config |
| HR | Employees, attendance, leave, overtime, HR support tickets |
| Finance | Payroll, payslips, payroll reports, finance support tickets |
| Employee | Attendance, profile, leave, overtime, personal payslips, support tickets |

Default role assignments in the seed data:

| Role | Employee Numbers |
| --- | --- |
| Admin | 10001, 10015 |
| Finance | 10010, 10011 |
| IT | 10005 |
| HR | 10006, 10007 |

---

## Login

Employee accounts are provisioned from employee records. The generated credential
format used by the seeded data is:

```text
Username: emp{EmployeeNumber}
Password: MotorPH@{EmployeeNumber}
```

Login is a two-step flow: credentials are verified first, then an OTP is
confirmed before any token is issued. If the account is still on a temporary
password, login stops there and the client is sent to the forced password-change
screen instead.

---

## Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8
- Git

For GitHub Actions database validation, the workflow starts its own MySQL 8
service container. For local development, run MySQL yourself.

---

## Database Setup

Create and load the database from the dump that exists in this repository:

```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS payrollsystem_db; CREATE DATABASE payrollsystem_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p --default-character-set=utf8mb4 < database/payrollsystem_db.sql
```

The application and backend expect the database name to be:

```text
payrollsystem_db
```

### Backend Database Account

The backend should not connect as `root`. Create a least-privilege account for it:

```sql
CREATE USER 'motorph_backend'@'127.0.0.1' IDENTIFIED BY '<strong-generated-password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON payrollsystem_db.* TO 'motorph_backend'@'127.0.0.1';
FLUSH PRIVILEGES;
```

No `GRANT OPTION`, no DDL rights, no access to other schemas, and the host is
scoped to `127.0.0.1` rather than `%`. Use a separate account for schema
migrations.

### Reporting Views and SQL Tests

The report pipeline uses these entry scripts:

```text
database/ci-cd/deploy_reports.sql
database/ci-cd/run_all_tests.sql
```

`deploy_reports.sql` loads the database dump, seeds missing BASIC_PAY earning
rows, and creates or refreshes these report views:

```text
vw_employee_payslip_report
vw_payroll_summary_monthly
```

`run_all_tests.sql` runs the report validation, edge-case, and performance SQL
checks from `database/sql_scripts/`.

To run the reporting database setup manually from a shell with the MySQL client:

```bash
cd database/ci-cd
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < deploy_reports.sql
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 --table < run_all_tests.sql
```

---

## Backend Setup

The backend reads its settings from environment variables. Secrets are never
committed; `application.yml` only holds placeholders.

Required:

```text
MOTORPH_DB_USER
MOTORPH_DB_PASSWORD
MOTORPH_JWT_SECRET          # 32+ bytes; startup fails if missing or too short
```

Optional:

```text
MOTORPH_DB_HOST=localhost
MOTORPH_DB_PORT=3306
SERVER_PORT=8080
MOTORPH_PROFILE=default
MOTORPH_JWT_ACCESS_EXPIRY_MINUTES=15
MOTORPH_JWT_REFRESH_EXPIRY_MINUTES=1440
MOTORPH_BACKUP_DIR=backups          # where database backups are written
MOTORPH_PHOTO_DIR=profile-photos    # where employee profile photos are stored
```

Run the backend:

```bash
cd motorph-backend
mvn spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

The backend is an API server, not a browser website. Opening
`http://localhost:8080/` directly may show `403 Forbidden` because `/` is secured
and there is no homepage route. That is normal. Use the JavaFX desktop app as the
main user interface, or call API endpoints such as `/api/v1/auth/login` with a
POST request.

If port 8080 is already in use, run the backend on another port:

```bash
export SERVER_PORT=8081
mvn spring-boot:run
```

> **Profile photos:** photos are stored in `MOTORPH_PHOTO_DIR` on the backend
> host and served over the API. Photos saved before this layout was introduced
> are recorded with their old client-side path; copy those image files into the
> photo directory once and they resolve by filename. Employees without a
> resolvable photo simply show their initials.

---

## Desktop App Setup

The desktop app uses the backend API at `http://localhost:8080` by default.
Start the backend first, then run the desktop app from the repository root:

```bash
mvn -pl motorph-desktop javafx:run
```

Or from inside the module:

```bash
cd motorph-desktop
mvn javafx:run
```

To point the desktop client at a different backend URL:

```bash
mvn -pl motorph-desktop javafx:run -Dmotorph.api.baseUrl=http://localhost:8081
```

With the backend stopped, the desktop app fails at the network boundary with a
connection error. It can no longer fall back to a direct database connection,
because it does not have one.

### HTTPS

Plain HTTP on `localhost` is a local-development convenience only. Any
deployment where the client and server are on different machines must run
behind TLS, and the desktop `HttpApiClient` must keep certificate validation
enabled. Treat this as a go-live requirement, not optional hardening.

---

## Testing

Run every test in the repository from the root:

```bash
mvn test
```

That builds both modules through the aggregator. To test one module on its own:

```bash
mvn -pl motorph-desktop test
mvn -pl motorph-backend test
```

Backend coverage focuses on the rules that are only enforced server-side:
permission gates on each endpoint, the account-enumeration contract of
`forgot-password`, backup filename validation against path traversal, profile
photo upload validation, and the temporary-password replay guard.

Run the database report validation locally with the MySQL client:

```bash
cd database/ci-cd
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < deploy_reports.sql
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 --table < run_all_tests.sql
```

---

## CI/CD

The repository includes a GitHub Actions workflow:

```text
.github/workflows/database-report-ci.yml
```

The workflow:

1. Starts a clean MySQL 8 service container.
2. Checks out the repository.
3. Runs `database/ci-cd/deploy_reports.sql`.
4. Runs `database/ci-cd/run_all_tests.sql`.
5. Fails if any report test returns `FAIL`.
6. Uploads `database/ci-cd/test_output.log` as an artifact.

The workflow validates the database layer only; it does not build or test the
Java modules.

The workflow depends on the SQL files in:

```text
database/payrollsystem_db.sql
database/ci-cd/
database/sql_scripts/
```

---

## Team

Developed as the AOOP-term group project for the MotorPH Payroll Management
System.

**H2101 - Group 5**

- **Joyce Mariel Poblete** - Project Lead / Integration
- **Harty Joy Villegas** - UI Lead
- **Erma Janna Alvester** - Backend & Reports
- **Mizzy Guaro** - HR UI
- **Shaira Marie Darantinao** - Finance UI
