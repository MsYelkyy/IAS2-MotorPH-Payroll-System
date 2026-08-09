# MotorPH Payroll Management System

MotorPH Payroll Management System is a Java 21 payroll and HR application built
for the Advanced Object-Oriented Programming (AOOP) subject. The project now has
a JavaFX desktop client, a Spring Boot REST backend, a MySQL 8 database, and a
database reporting CI pipeline.

The system supports employee management, attendance, leave and overtime requests,
payroll processing, payslip viewing, role-based access control, support tickets,
audit logs, database tools, and PDF report generation.

---

## Current Architecture

The codebase is split into three main areas:

```text
IAS2-MotorPH-Payroll-System/
|-- src/                         # JavaFX desktop client
|   |-- main/java/motorph/
|   |   |-- api/                 # REST API clients used by the UI
|   |   |-- app/                 # JavaFX application entry point
|   |   |-- application/         # Desktop use cases, DTOs, and services
|   |   |-- domain/              # Domain models, value objects, calculators
|   |   |-- infrastructure/      # Legacy JDBC repositories/config/reporting
|   |   `-- ui/                  # JavaFX controllers and UI helpers
|   `-- main/resources/          # FXML, CSS, images, fonts, report templates
|
|-- motorph-backend/             # Spring Boot REST API
|   |-- src/main/java/motorph/backend/
|   |   |-- configuration/       # Backend configuration
|   |   |-- controller/          # REST controllers under /api/v1
|   |   |-- domain/              # Backend domain model
|   |   |-- dto/                 # Request/response DTOs
|   |   |-- exception/           # Backend exceptions
|   |   |-- reporting/           # Server-side JasperReports support
|   |   |-- repository/          # JDBC repositories
|   |   |-- security/            # Spring Security, JWT, password handling
|   |   `-- service/             # Backend business services
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

The desktop app still contains some local service and JDBC classes because the
migration to the REST backend is in progress. New UI flows use the `motorph.api`
clients to call the Spring Boot backend at `http://localhost:8080` by default.

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
| Database | MySQL 8 |
| Data Access | JDBC, Spring JDBC, HikariCP |
| Reporting | SQL views, JasperReports 7.0.6, PDF export |
| Email | Jakarta Mail |
| Build Tool | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| CI | GitHub Actions with MySQL 8 service container |

---

## Main Features

- Role-based login for Admin, HR, Finance, IT, and Employee users
- Two-factor authentication and JWT-backed API sessions
- Employee profile, salary, benefits, and government details management
- Attendance time-in/time-out and attendance history
- Leave and overtime submission, approval, rejection, and cancellation
- Payroll and payslip viewing
- PDF reports through JasperReports
- Support ticket routing and resolution
- System configuration for government rates and tax brackets
- User access logs, role permissions, and database backup tools
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

The backend also supports OTP verification as part of the login flow.

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

The backend reads its database and JWT settings from environment variables.

Required:

```text
MOTORPH_DB_USER
MOTORPH_DB_PASSWORD
MOTORPH_JWT_SECRET
```

Optional:

```text
MOTORPH_DB_HOST=localhost
MOTORPH_DB_PORT=3306
SERVER_PORT=8080
MOTORPH_PROFILE=default
MOTORPH_JWT_ACCESS_EXPIRY_MINUTES=15
MOTORPH_JWT_REFRESH_EXPIRY_MINUTES=1440
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

---

## Desktop App Setup

The JavaFX desktop app uses the backend API at `http://localhost:8080` by
default. Start the backend first, then run the desktop app from the repository
root:

```bash
mvn clean javafx:run
```

To point the desktop client at a different backend URL:

```bash
mvn javafx:run -Dmotorph.api.baseUrl=http://localhost:8081
```

Some older desktop services still use the local JDBC settings in:

```text
src/main/java/motorph/infrastructure/config/DatabaseConnection.java
```

Default local values:

```java
private static final String URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
private static final String USER = "root";
private static final String PASSWORD = "root123";
```

---

## Testing

Run desktop/module unit tests from the repository root:

```bash
mvn test
```

Run backend tests:

```bash
cd motorph-backend
mvn test
```

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
