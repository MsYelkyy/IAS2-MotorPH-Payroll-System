-- =============================================================================
-- MotorPH Payroll System
-- Database Report CI/CD Pipeline  --  RUN ALL TESTS script
-- DBMS: MySQL 8.x
-- File: database/ci-cd/run_all_tests.sql
-- -----------------------------------------------------------------------------
-- What this does
--   Runs all report test scripts in one place, so the whole suite can be checked
--   with a single command -- locally in MySQL Workbench, or in the CI pipeline.
--   It covers, in order:
--     a. Employee Payslip validation tests   (DBT-###)
--     b. Payroll Summary validation tests    (DBM-###)
--     c. Edge-case tests                     (ECT-###)
--     d. Performance evidence                (data volume, view sizes, indexes)
--     e. Audit-log protection tests          (ALP-###)
--
-- Read-only
--   These tests only read the payroll data -- they never change it. Each suite
--   uses a TEMPORARY results table that is dropped when the session ends.
--   Suite (e) inserts one test log row and rolls it back.
--
-- Run this after deploy_reports.sql
--   The tests read the views and seeded data that deploy_reports.sql creates, so
--   run that first.
--
-- How pass/fail is decided
--   Each script prints a result grid where every row ends in PASS or FAIL, plus
--   a final verdict line. This file does not decide pass/fail itself. The CI
--   workflow saves all output to test_output.log and fails the build only on a
--   real failure: a FAIL result cell (shown as "| FAIL |") or a "TEST(S) FAILED"
--   verdict. It does not match the plain word "FAIL", because a passing run
--   still prints the lowercase "failed" column header and one label that reads
--   "...run seed_payroll_earning.sql if FAIL" -- neither of those is a failure.
--
-- Where to run it from
--   Like the deploy script, SOURCE reads the paths below relative to your current
--   folder. Run this from database/ci-cd/ (the CI workflow does).
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
SELECT '========== (e) AUDIT-LOG PROTECTION TESTS ==========' AS suite;
-- -----------------------------------------------------------------------------
SOURCE ../sql_scripts/audit_log_protection_tests.sql;

-- -----------------------------------------------------------------------------
SELECT '========== ALL TEST SCRIPTS EXECUTED ==========' AS suite;
