# MotorPH Payroll Management System

MotorPH Payroll Management System is a Java 21 payroll and HR application built as a multi-module system with a JavaFX desktop client, Spring Boot REST backend, and MySQL database.

The system supports employee management, attendance, leave and overtime, payroll processing, payslips, reports, role-based access control, audit logs, support tickets, and administrative tools.

---

## Architecture

```text
Admin / HR / Finance / IT / Employee
                  |
                  v
          JavaFX Desktop Client
          motorph-desktop
                  |
               REST API
                  |
                  v
          Spring Boot Backend
          motorph-backend
                  |
                  v
                MySQL
```

The JavaFX desktop does not connect directly to MySQL.

All authentication, authorization, validation, business rules, reporting, audit behavior, and database access are handled by the Spring Boot backend.

> **Desktop shows. Backend decides. Database stores.**

---

## Tech Stack

| Layer             | Technology                         |
| ----------------- | ---------------------------------- |
| Language          | Java 21                            |
| Desktop UI        | JavaFX 21, FXML, CSS               |
| Backend           | Spring Boot 3.5                    |
| API               | REST / HTTP / JSON                 |
| Security          | Spring Security, JWT, PBKDF2, OTP  |
| Database          | MySQL 8                            |
| Data Access       | JDBC, Spring JDBC, HikariCP        |
| Reporting         | JasperReports                      |
| Build             | Maven                              |
| Testing           | JUnit 5, Mockito, Spring Boot Test |
| Local Environment | Docker Compose                     |
| CI                | GitHub Actions                     |

---

# Getting Started

## Prerequisites

Install:

* **Git**
* **JDK 21**
* **Maven 3.9+**
* **Docker Desktop**

MySQL does **not** need to be installed separately when using Docker.

---

## 1. Clone the Repository

```bash
git clone https://github.com/MsYelkyy/IAS2-MotorPH-Payroll-System.git
cd IAS2-MotorPH-Payroll-System
```

---

## 2. Create Your Local Environment File

Copy the provided template:

### PowerShell

```powershell
Copy-Item .env.example .env
```

### Git Bash

```bash
cp .env.example .env
```

Open `.env` and replace all placeholder values.

At minimum, configure:

```env
MYSQL_ROOT_PASSWORD=<your-local-root-password>
MOTORPH_DB_PASSWORD=<your-local-backend-db-password>
MOTORPH_JWT_SECRET=<your-long-random-jwt-secret>
```

Do not commit `.env`.

Only `.env.example` should be stored in Git.

---

## 3. Start the Backend and Database

From the repository root:

```bash
docker compose up -d --build
```

Docker automatically:

* starts MySQL;
* creates `payrollsystem_db`;
* loads the database schema and seed data;
* applies required authentication tables;
* creates reporting views;
* creates the backend database account;
* starts the Spring Boot backend after MySQL becomes healthy.

Check the containers:

```bash
docker compose ps
```

Expected:

```text
motorph-mysql      healthy
motorph-backend    healthy
```

The backend is available at:

```text
http://localhost:8080
```

---

## 4. Run the JavaFX Desktop

The JavaFX application runs normally on the host and is **not containerized**.

From the repository root:

```bash
mvn -pl motorph-desktop javafx:run
```

The desktop automatically communicates with:

```text
http://localhost:8080
```

You should now be able to log in and use MotorPH normally.

---

# Daily Development

After the first setup, you normally do **not** need `--build` every time.

Start the containers:

```bash
docker compose up -d
```

Run the desktop:

```bash
mvn -pl motorph-desktop javafx:run
```

When finished:

```bash
docker compose stop
```

---

# Useful Docker Commands

### Check container status

```bash
docker compose ps
```

### View backend logs

```bash
docker compose logs -f backend
```

### View MySQL logs

```bash
docker compose logs -f mysql
```

### Stop containers

```bash
docker compose stop
```

### Stop and remove containers

Database data is preserved:

```bash
docker compose down
```

### Completely reset the local database

```bash
docker compose down -v
docker compose up -d --build
```

> `docker compose down -v` deletes the local Docker database volume and recreates the database from the repository initialization scripts on the next startup.

---

# Local Ports

| Service             | Address          |
| ------------------- | ---------------- |
| Spring Boot Backend | `localhost:8080` |
| MySQL               | `localhost:3307` |

The backend communicates with MySQL through Docker's internal network.

The JavaFX desktop communicates with the backend through `localhost:8080`.

---

# Repository Structure

```text
IAS2-MotorPH/
├── docker-compose.yml
├── .env.example
├── pom.xml
│
├── motorph-desktop/
│   ├── pom.xml
│   └── src/
│
├── motorph-backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│
├── database/
│   ├── payrollsystem_db.sql
│   ├── ci-cd/
│   └── sql_scripts/
│
├── docker/
│   └── mysql/
│       └── init/
│
└── .github/
    └── workflows/
```

### `motorph-desktop`

JavaFX desktop client used by:

* Admin
* HR
* Finance
* IT
* Employee

### `motorph-backend`

Spring Boot REST API responsible for:

* authentication;
* authorization;
* validation;
* payroll and business rules;
* reporting;
* audit logging;
* database access.

### `database`

Contains the MotorPH database schema, seed data, reporting views, and SQL validation scripts.

---

# Main Features

* Multi-role authentication
* OTP-based login
* Role-based access control
* Employee management
* Attendance
* Leave and overtime
* Payroll processing
* Payslips
* JasperReports PDF reports
* Employee self-service
* Support tickets
* Password reset workflow
* Audit logs
* Role and permission administration
* Database administration tools

---

# Testing

Run all Java tests:

```bash
mvn test
```

Desktop only:

```bash
mvn -pl motorph-desktop test
```

Backend only:

```bash
mvn -pl motorph-backend test
```

For API testing, the backend can be tested directly using tools such as Postman while the Docker stack is running.

---

# Troubleshooting

## Backend is still starting

Check:

```bash
docker compose ps
```

Then:

```bash
docker compose logs backend
```

Wait until the backend becomes healthy before launching or testing the desktop.

---

## Port 8080 is already in use

Stop the existing application using port `8080`, or configure a different backend port in your local `.env`.

---

## I changed the database initialization scripts but nothing changed

Docker initialization scripts run only when a new database volume is created.

Reset the local database:

```bash
docker compose down -v
docker compose up -d --build
```

---

## Desktop cannot connect to MotorPH

Make sure the backend container is running:

```bash
docker compose ps
```

and confirm the backend is available on:

```text
localhost:8080
```

---

# Security Notes

* Never commit `.env`.
* Never commit database passwords, JWT secrets, OTPs, or access tokens.
* The backend database account follows least-privilege access.
* The JavaFX client has no direct database credentials.
* Backend and MySQL Docker ports are bound to the local machine for development.
* Production deployment must use HTTPS/TLS.

---

# Team

**MotorPH Payroll Management System**

Developed as a group academic project and continued for Information Assurance and Security evaluation and refinement.
