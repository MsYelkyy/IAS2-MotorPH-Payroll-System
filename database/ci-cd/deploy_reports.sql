-- =============================================================================
-- MotorPH Payroll System
-- Database Report CI/CD Pipeline  --  DEPLOY script
-- DBMS: MySQL 8.x
-- File: database/ci-cd/deploy_reports.sql
-- -----------------------------------------------------------------------------
-- Purpose
--   Deploys the database reporting layer in one step, in the correct order.
--   Running this single file rebuilds the schema + data and (re)creates both
--   report views, so the reporting layer can be stood up with one command --
--   locally in MySQL Workbench, or automatically in the CI pipeline.
--
--   This is a BUILD/TEST deployment of the reporting layer on a fresh database.
--   It is NOT a production deployment and must not be run against live data.
--
-- Run order (each step depends on the ones before it)
--   1. ../payrollsystem_db.sql              -- schema + data (the project dump)
--   2. ../sql_scripts/seed_payroll_earning.sql
--                                       -- back-fills the BASIC_PAY earning rows
--                                       (the dump ships payroll_earning empty;
--                                        the report reconciliation needs this)
--   3. ../sql_scripts/employee_payslip_report_view.sql   -- per-payslip view
--   4. ../sql_scripts/payroll_summary_monthly_view.sql   -- monthly summary view
--
-- Re-run safety (idempotent where possible)
--   * The dump uses CREATE DATABASE IF NOT EXISTS and DROP TABLE IF EXISTS, so
--     re-running rebuilds the schema cleanly.
--   * Both views use DROP VIEW IF EXISTS + CREATE VIEW, so they refresh on
--     every run.
--   * The seed script is written to be safe to re-apply.
--   Net effect: this file can be re-run to reset the reporting layer to a known
--   good state.
--
-- How relative paths resolve
--   MySQL's SOURCE resolves paths from the client's CURRENT WORKING DIRECTORY,
--   not from this file's location. Run this script from the database/ci-cd/
--   folder so the "../..." paths below resolve correctly:
--       cd database/ci-cd
--       mysql -u <user> -p < deploy_reports.sql      (CLI), or
--       SOURCE deploy_reports.sql;                    (from a client already in
--                                                      this folder)
--   The CI workflow changes into database/ci-cd/ before calling this file.
--
--   Actual layout the paths below assume:
--       database/payrollsystem_db.sql        -> ../payrollsystem_db.sql
--       database/sql_scripts/<script>.sql    -> ../sql_scripts/<script>.sql
--       database/ci-cd/deploy_reports.sql    -> this file
--
-- Safety note
--   This rebuilds the schema/data and recreates the report views. It is meant
--   for a build/test/demo database -- do NOT run it against a database whose
--   data you want to keep.
-- =============================================================================

-- Step 1 -- Schema + data (project dump). Creates payrollsystem_db if missing.
SOURCE ../payrollsystem_db.sql;

-- Make sure every following script targets the right database, even if the
-- dump's own USE statement changes between versions.
USE payrollsystem_db;

-- Step 2 -- Back-fill BASIC_PAY earnings (report reconciliation depends on it).
--   The dump ships payroll_earning EMPTY, so this seed is required: without it,
--   basic_pay reads 0.00 in the views and the reconciliation tests (DBT-000,
--   DBT-004, DBT-011, DBM-004) FAIL. Keep this step.
SOURCE ../sql_scripts/seed_payroll_earning.sql;

-- Step 3 -- Employee Payslip report view (per-payslip / semi-monthly).
SOURCE ../sql_scripts/employee_payslip_report_view.sql;

-- Step 4 -- Payroll Summary report view (monthly roll-up).
SOURCE ../sql_scripts/payroll_summary_monthly_view.sql;

-- Deployment complete: schema + data loaded, BASIC_PAY seeded, both views built.
SELECT 'DEPLOY COMPLETE'                         AS status,
       (SELECT COUNT(*) FROM vw_employee_payslip_report) AS payslip_rows,
       (SELECT COUNT(*) FROM vw_payroll_summary_monthly) AS summary_rows;
