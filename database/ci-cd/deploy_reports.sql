-- =============================================================================
-- MotorPH Payroll System
-- Database Report CI/CD Pipeline  --  DEPLOY script
-- DBMS: MySQL 8.x
-- File: database/ci-cd/deploy_reports.sql
-- -----------------------------------------------------------------------------
-- What this does
--   Rebuilds the database reporting layer in one run. It loads the database,
--   seeds the data the reports need, and (re)creates both report views. Use it
--   to set up the reporting layer with a single command -- locally in MySQL
--   Workbench, or automatically in the CI pipeline.
--
--   This runs against a fresh test database only. It does NOT deploy to a live
--   production database, so do not run it on data you want to keep.
--
-- Run order (each step needs the ones before it)
--   1. Load the database dump (schema + data).
--   2. Seed the required BASIC_PAY earnings.
--   3. Create or refresh the Employee Payslip Report view.
--   4. Create or refresh the Payroll Summary Monthly view.
--
-- Safe to re-run
--   Every step can be run again without breaking anything. The dump recreates
--   the schema cleanly, the views are dropped and recreated, and the seed is
--   safe to re-apply. Re-running resets the reporting layer to a known good state.
--
-- Where to run it from
--   MySQL's SOURCE reads the paths below relative to your current folder, not to
--   this file. Run it from database/ci-cd/ so the "../..." paths resolve:
--       cd database/ci-cd
--       mysql -u <user> -p < deploy_reports.sql      (command line), or
--       SOURCE deploy_reports.sql;                    (in a client already here)
--   The CI workflow switches into database/ci-cd/ before running this file.
-- =============================================================================

-- Step 1 -- Load the database dump (schema + data). Creates payrollsystem_db.
SOURCE ../payrollsystem_db.sql;

-- Point the rest of the script at the right database, in case the dump's own
-- USE line changes in a later version.
USE payrollsystem_db;

-- Step 2 -- Seed the BASIC_PAY earnings. The dump ships this table empty, so
-- without the seed basic_pay shows 0.00 in the views and the reconciliation
-- tests (DBT-000, DBT-004, DBT-011, DBM-004) fail. Keep this step.
SOURCE ../sql_scripts/seed_payroll_earning.sql;

-- Step 3 -- Create or refresh the Employee Payslip Report view (per payslip).
SOURCE ../sql_scripts/employee_payslip_report_view.sql;

-- Step 4 -- Create or refresh the Payroll Summary Monthly view (monthly total).
SOURCE ../sql_scripts/payroll_summary_monthly_view.sql;

-- Done: database loaded, BASIC_PAY seeded, both views built. Row counts confirm
-- the views return data.
SELECT 'DEPLOY COMPLETE'                         AS status,
       (SELECT COUNT(*) FROM vw_employee_payslip_report) AS payslip_rows,
       (SELECT COUNT(*) FROM vw_payroll_summary_monthly) AS summary_rows;
