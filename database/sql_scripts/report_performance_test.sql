-- =============================================================================
-- MotorPH Payroll System
-- Report Performance Test
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/report_performance_test.sql
-- -----------------------------------------------------------------------------
-- Purpose
--   Performance evidence for the Employee Payslip Report
--   (vw_employee_payslip_report) and the Payroll Summary Report
--   (vw_payroll_summary_monthly). Checks data volume, report view row counts,
--   index coverage, and points to the execution-time and concurrency evidence.
--
-- Presentation output
--   Runs to just FOUR clean result grids by default, so the screen stays tidy:
--     1. performance_data_volume_summary  -- key table row counts
--     2. performance_report_view_summary  -- report view row counts
--     3. performance_index_summary        -- one index grid (replaces 8 SHOW INDEX tabs)
--     4. performance_test_summary         -- readable coverage summary
--
--   Optional detailed checks (kept commented out so they add no extra tabs):
--     * EXPLAIN ANALYZE plans (execution/runtime evidence) -- see bottom section
--     * mysqlslap concurrency test -- run separately (one session can't simulate
--       multiple users)
--
-- Safety note
--   Read-only. Only SELECTs and reads information_schema -- it does not change
--   any business data.
--
-- Prereqs (both report views must exist first):
--   1. SOURCE employee_payslip_report_view.sql;
--   2. SOURCE payroll_summary_monthly_view.sql;
--   3. SOURCE report_performance_test.sql;   (this file)
-- =============================================================================

USE payrollsystem_db;


-- =============================================================================
-- RESULT 1  --  performance_data_volume_summary
--   Row counts for the key tables behind the two report views -- the dataset
--   size the performance evidence is measured against. One grid.
-- =============================================================================
SELECT 'performance_data_volume_summary' AS result_set, 'employee'          AS table_name, COUNT(*) AS row_count FROM employee
UNION ALL
SELECT 'performance_data_volume_summary', 'payrun',             COUNT(*) FROM payrun
UNION ALL
SELECT 'performance_data_volume_summary', 'payroll_result',     COUNT(*) FROM payroll_result
UNION ALL
SELECT 'performance_data_volume_summary', 'payroll_earning',    COUNT(*) FROM payroll_earning
UNION ALL
SELECT 'performance_data_volume_summary', 'payroll_benefit',    COUNT(*) FROM payroll_benefit
UNION ALL
SELECT 'performance_data_volume_summary', 'payroll_deduction',  COUNT(*) FROM payroll_deduction
UNION ALL
SELECT 'performance_data_volume_summary', 'salary_history',     COUNT(*) FROM salary_history
UNION ALL
SELECT 'performance_data_volume_summary', 'government_details', COUNT(*) FROM government_details
ORDER BY table_name;


-- =============================================================================
-- RESULT 2  --  performance_report_view_summary
--   Number of rows each report view returns. One grid.
-- =============================================================================
SELECT 'performance_report_view_summary' AS result_set,
       'vw_employee_payslip_report'      AS report_view,
       COUNT(*)                          AS total_rows
FROM   vw_employee_payslip_report
UNION ALL
SELECT 'performance_report_view_summary',
       'vw_payroll_summary_monthly',
       COUNT(*)
FROM   vw_payroll_summary_monthly
ORDER BY report_view;


-- =============================================================================
-- RESULT 3  --  performance_index_summary
--   Index review for the tables used by the report views. Replaces eight
--   separate SHOW INDEX tabs with one grid from information_schema, so the index
--   evidence reads in a single place. One row per index column.
-- =============================================================================
SELECT
    'performance_index_summary'                 AS result_set,
    s.TABLE_NAME                                AS table_name,
    s.INDEX_NAME                                AS index_name,
    s.SEQ_IN_INDEX                              AS seq_in_index,
    s.COLUMN_NAME                               AS column_name,
    CASE WHEN s.NON_UNIQUE = 0 THEN 'UNIQUE' ELSE 'NON-UNIQUE' END AS uniqueness,
    s.INDEX_TYPE                                AS index_type
FROM   information_schema.STATISTICS s
WHERE  s.TABLE_SCHEMA = DATABASE()
  AND  s.TABLE_NAME IN (
        'employee', 'payrun', 'payroll_result', 'payroll_earning',
        'payroll_benefit', 'payroll_deduction', 'salary_history',
        'government_details'
       )
ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX;


-- =============================================================================
-- RESULT 4  --  performance_test_summary
--   Readable summary of the checks performed and where the detailed evidence is.
-- =============================================================================
SELECT
    'performance_test_summary'                              AS result_set,
    'Data volume checked'                                   AS check_1_data_volume,
    'View output size checked'                              AS check_2_view_output,
    'EXPLAIN ANALYZE prepared (Optional Detailed Analysis)' AS check_3_explain_analyze,
    'Index review available (performance_index_summary)'    AS check_4_index_review,
    'mysqlslap concurrency test available separately'       AS check_5_concurrency,
    'Employee Payslip Report and Payroll Summary Report'    AS reports_tested;


-- #############################################################################
-- OPTIONAL DETAILED ANALYSIS  --  supporting evidence (does NOT run by default)
-- -----------------------------------------------------------------------------
--   Everything below is commented out so the default run stays at 4 clean tabs.
--   These EXPLAIN ANALYZE statements show the execution plan and real runtime for
--   each report query. Run them one at a time so you add only the plan you want
--   to show, instead of many tabs at once.
--
--   HOW TO RUN: select the lines of one query below, remove the leading "-- ",
--   and execute just that selection.
-- #############################################################################

-- ---- A. Employee Payslip -- filtered (one employee, one month) --------------
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_employee_payslip_report
-- WHERE  employee_id = 10001
--   AND  period_start >= '2024-06-01'
--   AND  period_end   <= '2024-06-30';

-- ---- B. Employee Payslip -- multi-employee ordered sample -------------------
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_employee_payslip_report
-- ORDER BY employee_name, period_start
-- LIMIT 20;

-- ---- C. Employee Payslip -- full view extraction ----------------------------
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_employee_payslip_report;

-- ---- D. Payroll Summary -- limited sample -----------------------------------
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_payroll_summary_monthly
-- LIMIT 20;

-- ---- E. Payroll Summary -- filtered to June 2024 ----------------------------
--   Note: payroll_month is a 'YYYY-MM' text key, so filter on that format.
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_payroll_summary_monthly
-- WHERE  payroll_month = '2024-06';

-- ---- F. Payroll Summary -- full view extraction -----------------------------
-- EXPLAIN ANALYZE
-- SELECT *
-- FROM   vw_payroll_summary_monthly;

-- ---- G. Direct aggregation workload (bypasses the views) --------------------
-- EXPLAIN ANALYZE
-- SELECT
--     res.employee_id,
--     DATE_FORMAT(pr.end_date, '%Y-%m') AS payroll_month,
--     COUNT(*)                          AS payslip_count,
--     SUM(res.total_hours_worked)       AS total_hours_worked,
--     SUM(res.gross_pay)                AS total_gross_pay,
--     SUM(res.total_deductions)         AS total_deductions,
--     SUM(res.net_pay)                  AS total_net_pay
-- FROM   payroll_result res
-- JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
-- GROUP BY res.employee_id, DATE_FORMAT(pr.end_date, '%Y-%m');


-- #############################################################################
-- CONCURRENCY TEST  --  run separately (supporting evidence, not run here)
-- -----------------------------------------------------------------------------
--   One Workbench session runs a single query at a time, so it can't simulate
--   multiple users. Measure concurrency outside this script with mysqlslap
--   (bundled with MySQL). Example, from a shell -- not SQL:
--
--     mysqlslap --user=root --password ^
--       --create-schema=payrollsystem_db ^
--       --concurrency=25 --iterations=10 ^
--       --query="SELECT * FROM vw_payroll_summary_monthly WHERE payroll_month='2024-06';" ^
--       --delimiter=";"
--
--   Capture its min/avg/max timing as the concurrency evidence.
-- #############################################################################
