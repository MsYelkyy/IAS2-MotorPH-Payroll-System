# MotorPH Payroll Management System

A payroll and HR application for Admin, HR, Finance, IT, and Employee users.

Supports employee records, attendance, leave and overtime requests, payroll, payslips, reports, support tickets, and audit logs.

## Tech Stack

- Java 21 and JavaFX
- Spring Boot REST API
- MySQL 8
- Spring Security, JWT, and OTP
- JasperReports
- Maven, Docker Compose, and GitHub Actions

The desktop connects to the backend API. Only the backend connects to the database.

## Setup

### 1. Install the requirements

- Git
- JDK 21
- Maven 3.9+
- Docker Desktop

Docker runs MySQL, so you do not need to install it separately.

### 2. Clone the repository

```bash
git clone https://github.com/MsYelkyy/IAS2-MotorPH-Payroll-System.git
cd IAS2-MotorPH-Payroll-System
```

### 3. Configure your environment

Copy `.env.example` to `.env`.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Git Bash:

```bash
cp .env.example .env
```

Fill in the settings and passwords in `.env`. Do not commit this file or share its secrets.

### 4. Start the backend and database

Make sure Docker Desktop is running, then run:

```bash
docker compose up -d --build
```

On first setup, Docker loads the database schema and seed data, including authentication tables and role permissions. It also creates the report views and backend database account.

Check the services:

```bash
docker compose ps
```

Wait for MySQL and the backend to become healthy.

### 5. Run the desktop

```bash
mvn -pl motorph-desktop javafx:run
```

Log in with your company email and password, then complete OTP verification.

The JavaFX desktop runs on your computer, outside Docker.

## Daily Use

Start the services and desktop:

```bash
docker compose up -d
mvn -pl motorph-desktop javafx:run
```

Stop the services when finished:

```bash
docker compose stop
```

After changing backend code, rebuild it:

```bash
docker compose up -d --build backend
```

## Default Ports

| Service | Address |
| --- | --- |
| Backend | http://localhost:8080 |
| MySQL | localhost:3307 |

## Project Folders

| Folder | Contents |
| --- | --- |
| `motorph-desktop/` | JavaFX app |
| `motorph-backend/` | API and business logic |
| `database/` | Database dump, report views, and SQL tests |
| `docker/` | Database setup scripts |
| `.github/workflows/` | CI workflows |

## Tests

Run all Java tests:

```bash
mvn test
```

Run tests for one module:

```bash
mvn -pl motorph-backend test
mvn -pl motorph-desktop test
```

## Troubleshooting

### Desktop cannot connect

Check the services and backend logs:

```bash
docker compose ps
docker compose logs -f backend
```

Make sure the backend is healthy and port 8080 is available.

For database errors:

```bash
docker compose logs -f mysql
```

### Database changes are missing

The database dump and setup scripts run only when the database volume is empty. Rebuilding the backend does not reload them.

To start over with the current database dump:

**This deletes your local Docker database. Back up anything you need first.**

```bash
docker compose down -v
docker compose up -d --build
```

To remove containers while keeping the database, use `docker compose down` without `-v`.

## About

Developed as a group academic project and continued for Information Assurance and Security.