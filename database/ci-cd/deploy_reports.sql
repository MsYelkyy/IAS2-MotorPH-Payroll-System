-- MotorPH report setup for MySQL 8
-- Loads the test database, adds BASIC_PAY earnings, creates both report views,
-- and adds the audit-log protection triggers.
-- Use only with a test database. Running this again replaces existing data.
--
-- Run from database/ci-cd/ so the file paths work:
--   cd database/ci-cd
--   mysql -u <user> -p < deploy_reports.sql
-- Or, from a MySQL client opened in that folder:
--   SOURCE deploy_reports.sql;
-- The CI workflow uses the same folder.

-- Step 1: Load the database tables and data.
SOURCE ../payrollsystem_db.sql;

-- Use the payroll database for the remaining steps.
USE payrollsystem_db;

-- Step 2: Add BASIC_PAY earnings from payroll results.
-- Keep this step in case the database dump is missing these earnings.
SOURCE ../sql_scripts/seed_payroll_earning.sql;

-- Step 3: Create the employee payslip report view.
SOURCE ../sql_scripts/employee_payslip_report_view.sql;

-- Step 4: Create the monthly payroll summary view.
SOURCE ../sql_scripts/payroll_summary_monthly_view.sql;

-- Step 5: Block UPDATE and DELETE on user_access_logs.
SOURCE ../sql_scripts/audit_log_protection.sql;

-- Show the row counts to check that both report views contain data.
SELECT 'DEPLOY COMPLETE'                         AS status,
       (SELECT COUNT(*) FROM vw_employee_payslip_report) AS payslip_rows,
       (SELECT COUNT(*) FROM vw_payroll_summary_monthly) AS summary_rows;
