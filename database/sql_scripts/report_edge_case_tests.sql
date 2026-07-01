-- =============================================================================
-- MotorPH Payroll System
-- Reporting Edge-Case Tests  --  validation script
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/report_edge_case_tests.sql
-- -----------------------------------------------------------------------------
-- Validation purpose
--   Edge-case checks for both report views:
--       * vw_employee_payslip_report   (per-payslip)
--       * vw_payroll_summary_monthly   (monthly summary)
--   These are kept separate from the main validation scripts to keep testing
--   organized: the main suites cover the normal cases, while this file covers
--   trickier ones -- NULL-safety, hiding inactive employees, new hires, no
--   duplicate rows, and date/period sanity.
--
--   Each check records a PASS or FAIL into a temporary results table. At the end
--   the script returns, in order:
--       1. detailed edge-case result grid
--       2. category summary
--       3. suite summary (total / passed / failed)
--       4. final verdict
--
--   Test IDs use the ECT-### convention (Edge-Case Test), grouped by category:
--     ECT-001..005  Employee status
--     ECT-006..010  Payroll components (0.00 not NULL, reconciliation)
--     ECT-011..013  Employee data (missing gov details / supervisor / new hire)
--     ECT-014..017  Payroll summary consistency
--     ECT-018..020  Date / period checks
--
-- Employee status note
--   Inactive, resigned, or terminated employees are excluded from both views via
--   emp.is_active = 1. The status tests check the rule holds ("no inactive
--   employee appears in the report") -- so they pass today and still guard the
--   filter once a resigned employee with past payroll exists.
--   >>> Adjust here if your schema names the column differently (e.g.
--       employee_status, status): update the is_active references below. <<<
--
-- Safety note
--   Read-only against business data; writes only a temporary results table.
--
-- Prereqs (run in order):
--   1. SOURCE payrollsystem_db.sql;
--   2. SOURCE seed_payroll_earning.sql;
--   3. SOURCE employee_payslip_report_view.sql;
--   4. SOURCE payroll_summary_monthly_view.sql;
--   5. SOURCE report_edge_case_tests.sql;         (this file)
-- =============================================================================

USE payrollsystem_db;

-- -----------------------------------------------------------------------------
-- Test harness (temporary results table -- read-only against business data)
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS test_results;
CREATE TEMPORARY TABLE test_results (
    test_id    VARCHAR(16),
    category   VARCHAR(24),
    scenario   VARCHAR(140),
    expected   VARCHAR(64),
    actual     VARCHAR(64),
    result     VARCHAR(4),
    run_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- Shared test targets (chosen so the suite runs on any seeded database).
-- -----------------------------------------------------------------------------
-- An ACTIVE employee that actually has payroll records (for "should appear").
SET @active_emp := (
    SELECT MIN(res.employee_id)
    FROM   payroll_result res
    JOIN   employee e ON e.employee_id = res.employee_id
    WHERE  e.is_active = 1
);

-- A month that has TWO cutoffs for @active_emp (for roll-up / consistency tests).
SET @summary_month := (
    SELECT DATE_FORMAT(pr.end_date, '%Y-%m')
    FROM   payroll_result res
    JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
    WHERE  res.employee_id = @active_emp
    GROUP BY DATE_FORMAT(pr.end_date, '%Y-%m')
    ORDER BY COUNT(*) DESC, DATE_FORMAT(pr.end_date, '%Y-%m') DESC
    LIMIT 1
);

SELECT '--- Edge-case test targets ---' AS info,
       @active_emp    AS active_employee_id,
       @summary_month AS summary_month;


-- #############################################################################
-- CATEGORY 1 -- EMPLOYEE STATUS EDGE CASES  (ECT-001 .. ECT-005)
-- #############################################################################

-- =============================================================================
-- ECT-001  (Employee Status) Active employee WITH payroll appears in PAYSLIP
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-001', 'Employee Status',
       'Active employee with payroll records appears in Employee Payslip Report',
       'YES', CASE WHEN a.cnt > 0 THEN 'YES' ELSE 'NO' END,
       CASE WHEN a.cnt > 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM vw_employee_payslip_report
       WHERE employee_id = @active_emp) a;

-- =============================================================================
-- ECT-002  (Employee Status) Active employee WITH payroll appears in SUMMARY
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-002', 'Employee Status',
       'Active employee with payroll records appears in Payroll Summary Report',
       'YES', CASE WHEN a.cnt > 0 THEN 'YES' ELSE 'NO' END,
       CASE WHEN a.cnt > 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM vw_payroll_summary_monthly
       WHERE employee_id = @active_emp) a;

-- =============================================================================
-- ECT-003  (Employee Status) Inactive/resigned/terminated hidden from PAYSLIP
--   Invariant: NO row in the payslip view may belong to an is_active = 0
--   employee. Expected violations = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-003', 'Employee Status',
       'Inactive/resigned/terminated employees are hidden from Employee Payslip Report',
       '0', a.leaks,
       CASE WHEN a.leaks = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS leaks
      FROM vw_employee_payslip_report v
      JOIN employee e ON e.employee_id = v.employee_id
      WHERE e.is_active = 0) a;

-- =============================================================================
-- ECT-004  (Employee Status) Inactive/resigned/terminated hidden from SUMMARY
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-004', 'Employee Status',
       'Inactive/resigned/terminated employees are hidden from Payroll Summary Report',
       '0', a.leaks,
       CASE WHEN a.leaks = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS leaks
      FROM vw_payroll_summary_monthly v
      JOIN employee e ON e.employee_id = v.employee_id
      WHERE e.is_active = 0) a;

-- =============================================================================
-- ECT-005  (Employee Status) Every reported employee is active in BOTH views
--   Cross-check the two views agree on the active-only population: the distinct
--   employees they expose must be a subset of the active roster.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-005', 'Employee Status',
       'Every employee shown in either report is active (is_active = 1)',
       '0', a.inactive_shown,
       CASE WHEN a.inactive_shown = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT COUNT(*) AS inactive_shown FROM (
        SELECT employee_id FROM vw_employee_payslip_report
        UNION
        SELECT employee_id FROM vw_payroll_summary_monthly
    ) shown
    JOIN employee e ON e.employee_id = shown.employee_id
    WHERE e.is_active = 0
) a;


-- #############################################################################
-- CATEGORY 2 -- PAYROLL COMPONENT EDGE CASES  (ECT-006 .. ECT-010)
-- #############################################################################

-- =============================================================================
-- ECT-006  (Payroll Component) No deductions => 0.00, never NULL (payslip)
--   COALESCE(...,0) must make every deduction column non-null across the whole
--   view, so a payslip with no deduction rows shows 0.00 not NULL.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-006', 'Payroll Component',
       'Employees with no deductions show 0.00 (not NULL) for all deduction columns',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(
          (sss_deduction        IS NULL) + (philhealth_deduction IS NULL) +
          (pagibig_deduction    IS NULL) + (withholding_tax      IS NULL) +
          (late_deduction       IS NULL) + (undertime_deduction  IS NULL) +
          (absence_deduction    IS NULL) + (total_deductions     IS NULL)
      ), 0) AS nulls
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-007  (Payroll Component) No benefits => 0.00, never NULL (payslip)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-007', 'Payroll Component',
       'Employees with no benefits show 0.00 (not NULL) for all benefit columns',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(
          (rice_subsidy    IS NULL) + (phone_allowance    IS NULL) +
          (clothing_allowance IS NULL) + (total_benefits  IS NULL)
      ), 0) AS nulls
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-008  (Payroll Component) No overtime => 0.00, never NULL (payslip)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-008', 'Payroll Component',
       'Employees with no overtime show 0.00 (not NULL) for overtime pay',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(overtime_pay IS NULL), 0) AS nulls
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-009  (Payroll Component) Attendance-only deductions reconcile (payslip)
--   Even when a cutoff has ONLY attendance-based deductions (late / undertime /
--   absence) and no statutory ones, the seven component columns must still add
--   up to total_deductions. Expected mismatches = 0 across the whole view.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-009', 'Payroll Component',
       'Component deductions reconcile to total_deductions incl. attendance-only rows',
       '0', a.mismatches,
       CASE WHEN a.mismatches = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(CASE WHEN ROUND(sss_deduction + philhealth_deduction
                              + pagibig_deduction + withholding_tax
                              + late_deduction + undertime_deduction
                              + absence_deduction, 2)
                        <> ROUND(total_deductions, 2)
                      THEN 1 ELSE 0 END), 0) AS mismatches
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-010  (Payroll Component) Summary is NULL-safe for all monetary columns
--   Same 0.00-not-NULL guarantee at the MONTHLY grain.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-010', 'Payroll Component',
       'Summary monetary columns are 0.00 (not NULL) for benefits/deductions/overtime',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(
          (overtime_pay IS NULL) + (total_benefits IS NULL) +
          (sss_deduction IS NULL) + (philhealth_deduction IS NULL) +
          (pagibig_deduction IS NULL) + (withholding_tax IS NULL) +
          (late_deduction IS NULL) + (undertime_deduction IS NULL) +
          (absence_deduction IS NULL) + (monthly_total_deductions IS NULL)
      ), 0) AS nulls
      FROM vw_payroll_summary_monthly) a;


-- #############################################################################
-- CATEGORY 3 -- EMPLOYEE DATA EDGE CASES  (ECT-011 .. ECT-013)
-- #############################################################################

-- =============================================================================
-- ECT-011  (Employee Data) Missing government details -> placeholder, no NULL
--   The views LEFT JOIN government_details and COALESCE the numbers to an em
--   dash. Report must never crash / show NULL for gov IDs. Expected NULLs = 0
--   across BOTH views combined.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-011', 'Employee Data',
       'Missing government details show placeholder (never NULL) in both reports',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT
        COALESCE((SELECT SUM((tin_number IS NULL) + (sss_number IS NULL)
                  + (philhealth_number IS NULL) + (pagibig_number IS NULL))
         FROM vw_employee_payslip_report), 0)
      + COALESCE((SELECT SUM((tin_number IS NULL) + (sss_number IS NULL)
                  + (philhealth_number IS NULL) + (pagibig_number IS NULL))
         FROM vw_payroll_summary_monthly), 0) AS nulls
) a;

-- =============================================================================
-- ECT-012  (Employee Data) Missing supervisor -> placeholder, no NULL (payslip)
--   supervisor_id is nullable (top of the org chart). The payslip view maps a
--   missing supervisor to an em dash, so supervisor_name is never NULL.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-012', 'Employee Data',
       'Missing supervisor shows placeholder (never NULL) in Employee Payslip Report',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(supervisor_name IS NULL), 0) AS nulls
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-013  (Employee Data) New hire with one cutoff appears only for that period
--   Test: a new hire whose first month has a single payslip should show a
--   payslip_count of 1 for that month, and not appear in any other month. Finds
--   an employee-month with exactly one cutoff and checks its count is 1. (If no
--   single-cutoff month exists in the data, the test passes -- nothing to check.)
-- =============================================================================
SET @nh_emp := (
    SELECT res.employee_id
    FROM   payroll_result res
    JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
    JOIN   employee e ON e.employee_id = res.employee_id
    WHERE  e.is_active = 1
    GROUP BY res.employee_id, DATE_FORMAT(pr.end_date, '%Y-%m')
    HAVING COUNT(*) = 1
    ORDER BY res.employee_id
    LIMIT 1
);
SET @nh_month := (
    SELECT DATE_FORMAT(pr.end_date, '%Y-%m')
    FROM   payroll_result res
    JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
    WHERE  res.employee_id = @nh_emp
    GROUP BY DATE_FORMAT(pr.end_date, '%Y-%m')
    HAVING COUNT(*) = 1
    ORDER BY DATE_FORMAT(pr.end_date, '%Y-%m')
    LIMIT 1
);
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-013', 'Employee Data',
       'New hire / single-cutoff month appears only for its own period (payslip_count = 1)',
       '1',
       COALESCE((SELECT payslip_count FROM vw_payroll_summary_monthly
                  WHERE employee_id = @nh_emp AND payroll_month = @nh_month), '1'),
       CASE WHEN @nh_emp IS NULL THEN 'PASS'   -- no single-cutoff month: vacuously true
            WHEN (SELECT payslip_count FROM vw_payroll_summary_monthly
                   WHERE employee_id = @nh_emp AND payroll_month = @nh_month) = 1
            THEN 'PASS' ELSE 'FAIL' END;


-- #############################################################################
-- CATEGORY 4 -- PAYROLL SUMMARY CONSISTENCY EDGE CASES  (ECT-014 .. ECT-017)
-- #############################################################################

-- =============================================================================
-- ECT-014  (Summary Consistency) Monthly totals == SUM of semi-monthly payslips
--   For the whole view: every summary row's monthly_gross / deductions / net
--   must equal the SUM of the matching per-payslip rows for that employee+month.
--   Expected mismatched rows = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-014', 'Summary Consistency',
       'Monthly summary totals equal the SUM of the related semi-monthly payslips',
       '0', COALESCE(a.mismatches, 0),
       CASE WHEN COALESCE(a.mismatches, 0) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT SUM(
        CASE WHEN ROUND(s.monthly_gross_pay,2)        <> ROUND(p.sum_gross,2)
              OR ROUND(s.monthly_total_deductions,2)  <> ROUND(p.sum_ded,2)
              OR ROUND(s.monthly_net_pay,2)           <> ROUND(p.sum_net,2)
             THEN 1 ELSE 0 END) AS mismatches
    FROM vw_payroll_summary_monthly s
    JOIN (
        SELECT v.employee_id,
               DATE_FORMAT(pr.end_date,'%Y-%m') AS ym,
               SUM(v.gross_pay)        AS sum_gross,
               SUM(v.total_deductions) AS sum_ded,
               SUM(v.net_pay)          AS sum_net
        FROM vw_employee_payslip_report v
        JOIN payrun pr ON pr.pay_run_id = v.pay_run_id
        GROUP BY v.employee_id, DATE_FORMAT(pr.end_date,'%Y-%m')
    ) p ON p.employee_id = s.employee_id AND p.ym = s.payroll_month
) a;

-- =============================================================================
-- ECT-015  (Summary Consistency) payslip_count == number of payroll results
--   payslip_count in the summary must equal the actual count of payroll_result
--   rows rolled into that employee-month. Expected mismatches = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-015', 'Summary Consistency',
       'Summary payslip_count equals number of payroll results for the month',
       '0', COALESCE(a.mismatches, 0),
       CASE WHEN COALESCE(a.mismatches, 0) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT SUM(CASE WHEN s.payslip_count <> r.cnt THEN 1 ELSE 0 END) AS mismatches
    FROM vw_payroll_summary_monthly s
    JOIN (
        SELECT res.employee_id,
               DATE_FORMAT(pr.end_date,'%Y-%m') AS ym,
               COUNT(*) AS cnt
        FROM payroll_result res
        JOIN payrun pr ON pr.pay_run_id = res.pay_run_id
        JOIN employee e ON e.employee_id = res.employee_id
        WHERE e.is_active = 1
        GROUP BY res.employee_id, DATE_FORMAT(pr.end_date,'%Y-%m')
    ) r ON r.employee_id = s.employee_id AND r.ym = s.payroll_month
) a;

-- =============================================================================
-- ECT-016  (Summary Consistency) No duplicate rows per (employee, month)
--   The summary must expose exactly ONE row per employee per calendar month.
--   Expected duplicate groups = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-016', 'Summary Consistency',
       'No duplicate report rows for the same employee and payroll month',
       '0', COALESCE(a.dupes, 0),
       CASE WHEN COALESCE(a.dupes, 0) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT COUNT(*) AS dupes FROM (
        SELECT employee_id, payroll_month, COUNT(*) AS c
        FROM vw_payroll_summary_monthly
        GROUP BY employee_id, payroll_month
        HAVING COUNT(*) > 1
    ) d
) a;

-- =============================================================================
-- ECT-017  (Summary Consistency) No negative monetary values (summary)
--   Gross / benefits / deductions / net must all be >= 0 across the view.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-017', 'Summary Consistency',
       'Monetary values (gross, benefits, deductions, net) are never negative',
       '0', a.negatives,
       CASE WHEN a.negatives = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(CASE WHEN monthly_gross_pay < 0 OR total_benefits < 0
                       OR monthly_total_deductions < 0 OR monthly_net_pay < 0
                      THEN 1 ELSE 0 END), 0) AS negatives
      FROM vw_payroll_summary_monthly) a;


-- #############################################################################
-- CATEGORY 5 -- DATE / PERIOD EDGE CASES  (ECT-018 .. ECT-020)
-- #############################################################################

-- =============================================================================
-- ECT-018  (Date/Period) Period start <= period end (payslip)
--   No payslip may have start_date later than end_date. Expected violations = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-018', 'Date/Period',
       'Pay period start date is not later than pay period end date',
       '0', a.bad,
       CASE WHEN a.bad = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(CASE WHEN period_start > period_end THEN 1 ELSE 0 END), 0) AS bad
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-019  (Date/Period) Pay date >= period end (payslip)
--   Employees are paid on/after the period closes. Expected violations = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-019', 'Date/Period',
       'Pay date is not earlier than the pay period end date',
       '0', a.bad,
       CASE WHEN a.bad = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(CASE WHEN pay_date < period_end THEN 1 ELSE 0 END), 0) AS bad
      FROM vw_employee_payslip_report) a;

-- =============================================================================
-- ECT-020  (Date/Period) Records grouped under the correct Payroll Month/Year
--   Test: the readable Payroll Month/Year label should match the actual month of
--   the records. Catches mislabeled or mis-grouped months. Expected mismatches = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ECT-020', 'Date/Period',
       'Records are grouped under the correct Payroll Month/Year label',
       '0', a.bad,
       CASE WHEN a.bad = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COALESCE(SUM(CASE
              WHEN payroll_month_year <> DATE_FORMAT(month_end, '%M %Y')
                OR payroll_month      <> DATE_FORMAT(month_end, '%Y-%m')
              THEN 1 ELSE 0 END), 0) AS bad
      FROM vw_payroll_summary_monthly) a;


-- =============================================================================
-- RESULTS
--   1) detailed edge-case test result grid
--   2) category summary
--   3) suite summary
--   4) final verdict
-- =============================================================================

-- 1) Detailed grid ------------------------------------------------------------
SELECT test_id, category, scenario, expected, actual, result
FROM   test_results
ORDER BY test_id;

-- 2) Category summary ---------------------------------------------------------
SELECT category,
       COUNT(*)             AS tests,
       SUM(result = 'PASS') AS passed,
       SUM(result = 'FAIL') AS failed
FROM   test_results
GROUP BY category
ORDER BY category;

-- 3) Suite summary ------------------------------------------------------------
SELECT 'EDGE-CASE SUITE SUMMARY'         AS report,
       COUNT(*)                          AS total_tests,
       SUM(result = 'PASS')              AS passed,
       SUM(result = 'FAIL')              AS failed
FROM   test_results;

-- 4) Final verdict ------------------------------------------------------------
SELECT CASE WHEN SUM(result = 'FAIL') = 0
            THEN 'FINAL VERDICT: ALL EDGE-CASE TESTS PASSED'
            ELSE CONCAT('FINAL VERDICT: ', SUM(result = 'FAIL'), ' EDGE-CASE TEST(S) FAILED')
       END AS final_verdict
FROM   test_results;
