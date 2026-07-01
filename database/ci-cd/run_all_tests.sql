-- =============================================================================
-- MotorPH Payroll System
-- Database Report CI/CD Pipeline  --  RUN ALL TESTS script
-- DBMS: MySQL 8.x
-- File: database/ci-cd/run_all_tests.sql
-- -----------------------------------------------------------------------------
-- Purpose
--   Runs every report validation and supporting test script in one pass, so the
--   whole reporting test suite can be executed with a single command -- locally
--   in MySQL Workbench, or automatically in the CI pipeline.
--
--   Order:
--     a. Employee Payslip validation tests   (DBT-###)
--     b. Payroll Summary validation tests     (DBM-###)
--     c. Edge-case tests                       (ECT-###)
--     d. Performance evidence script           (data volume, view sizes, indexes)
--
-- How the CI pass/fail gate works
--   Each validation script prints a result grid where every row ends in PASS or
--   FAIL, plus a final verdict line. This file does NOT decide pass/fail itself.
--   Instead, the CI workflow captures ALL output to test_output.log and FAILS
--   the build only on a REAL failure -- a FAIL result cell in a table grid
--   (matched as "| FAIL |") or a "TEST(S) FAILED" verdict line. That keeps the
--   pass/fail gate outside the SQL, so the test scripts stay unchanged.
--
--   Note: the gate deliberately does NOT match on the bare word "FAIL", because
--   a passing run still prints the lowercase "failed" column header and one
--   scenario label that literally contains the word FAIL
--   ("...run seed_payroll_earning.sql if FAIL"). Those are not failures.
--
-- Prerequisite
--   Run deploy_reports.sql first (schema + data + seed/fix + both views). These
--   tests read those views and the seeded data.
--
-- Relative paths
--   Like the deploy script, SOURCE resolves paths from the client's current
--   working directory. Run this from database/ci-cd/ (the CI workflow does).
--
-- Safety note
--   Read-only against business data. Each suite writes only a TEMPORARY results
--   table, dropped at session end. No INSERT/UPDATE/DELETE on payroll tables.
-- =============================================================================

USE payrollsystem_db;

-- -----------------------------------------------------------------------------
SELECT '========== (a) EMPLOYEE PAYSLIP VALIDATION TESTS ==========' AS suite;
-- -----------------------------------------------------------------------------
SOURCE ../sql_scripts/employee_payslip_report_tests.sql;

-- -----------------------------------------------------------------------------
SELECT '========== (b) PAYROLL SUMMARY VALIDATION TESTS ==========' AS suite;
-- -----------------------------------------------------------------------------
SOURCE ../sql_scripts/payroll_summary_report_tests.sql;

-- -----------------------------------------------------------------------------
SELECT '========== (c) REPORTING EDGE-CASE TESTS ==========' AS suite;
-- -----------------------------------------------------------------------------
SOURCE ../sql_scripts/report_edge_case_tests.sql;

-- -----------------------------------------------------------------------------
SELECT '========== (d) PERFORMANCE EVIDENCE ==========' AS suite;
-- -----------------------------------------------------------------------------
SOURCE ../sql_scripts/report_performance_test.sql;

-- -----------------------------------------------------------------------------
SELECT '========== ALL TEST SCRIPTS EXECUTED ==========' AS suite;
