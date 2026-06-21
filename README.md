# MotorPH Payroll Management System

MotorPH Payroll Management System is a Java desktop application built for the
**Advanced Object-Oriented Programming (AOOP)** subject. It is the evolution of our
previous OOP-term project, re-engineered with a clean, layered architecture and a
modern desktop tech stack.

It is designed to manage employee records, payroll processing, and role-based access
for MotorPH — now backed by a real **MySQL** database accessed through **JDBC**.

---

## What's New in the AOOP Version

Compared to the previous OOP-term project, this version was rebuilt to use:

- **JavaFX + FXML** for the user interface (replacing the old console/Swing UI)
- **MySQL** as the database, accessed directly through **JDBC** (replacing flat-file/CSV storage)
- **Maven** for dependency management and builds
- A **layered architecture** (`domain` / `application` / `infrastructure` / `ui`)
- **JasperReports** for PDF payslip and report generation
- **PBKDF2** salted password hashing and **two-factor authentication**
- **Email notifications** via Jakarta Mail
- **JUnit 5 + Mockito** for unit testing

---

## System Overview

This application supports core HR and Payroll operations, including:

- Employee record management
- Attendance tracking
- Leave request processing
- Overtime request processing
- Payroll generation and payslip viewing
- Role-based access control and permission management
- User access logging and audit trails
- Database backup and recovery

The system demonstrates key OOP and AOOP concepts including:

- **Encapsulation**
- **Inheritance**
- **Polymorphism**
- **Abstraction**
- Layered (n-tier) architecture and separation of concerns
- Dependency injection through service factories
- Repository pattern for data access

---

## Tech Stack

| Layer / Concern    | Technology                          |
| ------------------ | ----------------------------------- |
| Language           | Java 21                             |
| UI                 | JavaFX 21 + FXML + CSS              |
| Database           | MySQL 8                             |
| Data Access        | JDBC (`mysql-connector-j`)         |
| Build Tool         | Maven                               |
| Reporting          | JasperReports (PDF export)          |
| Email              | Jakarta Mail                        |
| Security           | PBKDF2 (`PBKDF2WithHmacSHA256`)    |
| Testing            | JUnit 5, Mockito                    |
| IDE                | IntelliJ IDEA                       |

---

## Project Structure

The codebase follows a layered architecture under `src/main/java/motorph`:

```text
motorph
├── app              # Application entry point (JavaFX Main)
├── domain           # Core business model — entities, value objects, calculators, contracts
│   ├── model        # Employee, Payslip, LeaveRequest, Attendance, Permission, ...
│   ├── value        # Value objects (Role, PermissionKey, ...)
│   ├── calculator   # Payroll/deduction calculation logic
│   └── contract     # Domain interfaces
├── application      # Use cases and orchestration
│   ├── service      # AuthenticationService, PayrollService, EmployeeService, ...
│   ├── dto          # Data transfer objects
│   ├── security     # PasswordHasher (PBKDF2)
│   ├── session      # Logged-in user session handling
│   └── exception    # Application-level exceptions
├── infrastructure   # External-facing implementations
│   ├── config       # DatabaseConnection (JDBC)
│   ├── repository   # JDBC repositories (data access via prepared statements)
│   └── reporting    # JasperReportGenerator
└── ui               # JavaFX controllers and view logic
    ├── auth         # Login / authentication screens
    ├── admin        # Payroll administrator views
    ├── hr           # HR views
    ├── finance      # Finance views
    ├── it           # IT / system administrator views
    ├── employee     # Employee self-service views
    ├── reports      # Report viewing/export
    ├── factory      # UI service factories (dependency wiring)
    └── util         # Shared UI helpers
```

Resources (FXML, CSS, fonts, images, report templates) live under `src/main/resources`.

---

## User Roles & Features

### Admin (Payroll Administrator)

- Full system access
- Manage company settings
- View all reports and system data
- Manage employees, payroll, and leave records

### IT (System Administrator)

- Create and manage user accounts
- Manage roles and permissions
- View user access logs
- Perform database backup and recovery
- Maintain system configuration

### HR

- Add, update, and delete employee records
- View attendance records of all employees
- Approve or reject leave and overtime requests

### Finance

- Generate payroll and pay runs
- View and print payslips of all employees

### Employee

- Record time-in and time-out (attendance)
- View personal profile information
- Submit leave and overtime requests
- View and print personal payslip

---

## Login & Access Control

The system uses **role-based authentication** with **salted password hashing (PBKDF2,
`PBKDF2WithHmacSHA256`)** and **two-factor authentication**.

### Employee Login

User accounts are automatically provisioned upon first login.

Employee login format:

```text
Username: emp{EmployeeNumber}
Password: MotorPH@{EmployeeNumber}
```

### Default System Accounts

The following employee numbers are assigned to specific system roles:

| Role    | Employee Numbers |
| ------- | ---------------- |
| Admin   | 10001, 10015     |
| Finance | 10010, 10011     |
| IT      | 10005            |
| HR      | 10006, 10007     |

---

## Getting Started

### Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **MySQL 8** running locally

### 1. Set Up the Database

Create the database and import the provided dump:

```bash
mysql -u root -p -e "CREATE DATABASE motorph_db;"
mysql -u root -p motorph_db < database/motorph_db_updated_dump.sql
```

### 2. Configure the Connection

Database connection settings are defined in
`src/main/java/motorph/infrastructure/config/DatabaseConnection.java`.
Update the URL, username, and password to match your local MySQL setup:

```java
private static final String URL = "jdbc:mysql://localhost:3306/motorph_db";
private static final String USER = "root";
private static final String PASSWORD = "root123";
```

### 3. Run the Application

```bash
mvn clean javafx:run
```

### 4. Run the Tests

```bash
mvn test
```

---

## Team

Developed as the AOOP-term group project for the MotorPH Payroll Management System.

**H2101 - Group 5**

- **Joyce Mariel Poblete** — Project Lead / Integration
- **Harty Joy Villegas** — UI Lead
- **Erma Janna Alvester** — Backend & Reports
- **Mizzy Guaro** — HR UI
- **Shaira Marie Darantinao** — Finance UI
