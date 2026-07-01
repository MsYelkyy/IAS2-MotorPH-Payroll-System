-- =============================================================================
-- MotorPH Payroll System
-- Employee Payslip Report  --  validation script
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/employee_payslip_report_tests.sql
-- -----------------------------------------------------------------------------
-- Validation purpose
--   Automated checks for the Employee Payslip Report view
--   (vw_employee_payslip_report). Each check records a PASS or FAIL into a
--   temporary results table, and a final summary reports the total passed and
--   failed with one overall verdict -- so the whole suite is read in one place.
--
--   Test IDs use the DBT-### convention, grouped by category:
--     * Positive      -- expected data is present and correct
--     * Negative      -- referential integrity (no orphan rows)
--     * Boundary      -- limit values (non-negative, tax floor)
--     * Edge Case     -- missing optional data does not break the payslip
--     * Business Rule -- payroll identities (net = gross - deductions, etc.)
--
-- Safety note
--   Read-only against business data. The only thing it writes is a temporary
--   results table (dropped at session end) -- no INSERT/UPDATE/DELETE on any
--   payroll table.
--
-- Prereqs (run in order):
--   1. SOURCE payrollsystem_db.sql;              (schema + data)
--   2. SOURCE seed_payroll_earning.sql;          (back-fills BASIC_PAY)
--   3. SOURCE employee_payslip_report_view.sql;  (rebuilds the view)
--   4. SOURCE employee_payslip_report_tests.sql; (this file)
-- =============================================================================

USE payrollsystem_db;

-- -----------------------------------------------------------------------------
-- Test harness
--   A temporary results table collects each check. Every test computes actual
--   vs expected and records PASS or FAIL; the summary at the end adds them up.
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS test_results;
CREATE TEMPORARY TABLE test_results (
    test_id    VARCHAR(16),
    category   VARCHAR(20),
    scenario   VARCHAR(120),
    expected   VARCHAR(64),
    actual     VARCHAR(64),
    result     VARCHAR(4),
    run_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- Test target: the lowest existing (employee_id, pay_run_id) pair, so the suite
-- runs on any seeded database without hardcoding ids that might not exist.
-- -----------------------------------------------------------------------------
SET @test_result_id := (SELECT MIN(payroll_result_id) FROM payroll_result);
SET @test_emp       := (SELECT employee_id FROM payroll_result WHERE payroll_result_id = @test_result_id);
SET @test_run       := (SELECT pay_run_id  FROM payroll_result WHERE payroll_result_id = @test_result_id);


-- =============================================================================
-- DBT-000  (Precondition) BASIC_PAY earnings have been seeded
--   The reconciliation tests (DBT-004, DBT-011) need BASIC_PAY to be present.
--   If this FAILs, run  SOURCE seed_payroll_earning.sql;  then re-run this file.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-000', 'Precondition',
       'BASIC_PAY earnings seeded (run seed_payroll_earning.sql if FAIL)',
       'all results', CONCAT(a.seeded, '/', a.total),
       CASE WHEN a.seeded = a.total AND a.total > 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT (SELECT COUNT(*) FROM payroll_result)                       AS total,
           (SELECT COUNT(DISTINCT pe.payroll_result_id)
              FROM payroll_earning pe
              JOIN earning_type et ON et.earning_type_id = pe.earning_type_id
             WHERE et.code = 'BASIC_PAY')                              AS seeded
) a;

-- =============================================================================
-- DBT-001  (Positive) View returns exactly one row per payslip
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-001', 'Positive',
       'View returns exactly one row for the target payslip',
       '1', a.cnt,
       CASE WHEN a.cnt = 1 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM vw_employee_payslip_report
       WHERE employee_id = @test_emp AND pay_run_id = @test_run) a;

-- =============================================================================
-- DBT-002  (Business Rule) net_pay = gross_pay - total_deductions
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-002', 'Business Rule',
       'Stored net_pay equals gross_pay minus total_deductions',
       ROUND(gross_pay - total_deductions, 2), ROUND(net_pay, 2),
       CASE WHEN ROUND(net_pay,2) = ROUND(gross_pay - total_deductions,2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-003  (Business Rule) Component deductions reconcile to total_deductions
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-003', 'Business Rule',
       'Component deductions reconcile to stored total_deductions',
       ROUND(total_deductions, 2),
       ROUND(sss_deduction + philhealth_deduction + pagibig_deduction
             + withholding_tax + late_deduction + undertime_deduction
             + absence_deduction, 2),
       CASE WHEN ROUND(sss_deduction + philhealth_deduction + pagibig_deduction
                       + withholding_tax + late_deduction + undertime_deduction
                       + absence_deduction, 2) = ROUND(total_deductions, 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-004  (Business Rule) Earnings + benefits reconcile to stored gross_pay
--   NOTE: depends on seed_payroll_earning.sql having back-filled BASIC_PAY.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-004', 'Business Rule',
       'Earnings + benefits reconcile to stored gross_pay',
       ROUND(gross_pay, 2), ROUND(total_gross_compensation, 2),
       CASE WHEN ROUND(total_gross_compensation, 2) = ROUND(gross_pay, 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-005  (Boundary) All monetary payslip fields are non-negative
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-005', 'Boundary',
       'All monetary payslip fields are non-negative',
       'PASS',
       CASE WHEN basic_pay >= 0 AND overtime_pay >= 0
             AND rice_subsidy >= 0 AND phone_allowance >= 0 AND clothing_allowance >= 0
             AND sss_deduction >= 0 AND philhealth_deduction >= 0 AND pagibig_deduction >= 0
             AND withholding_tax >= 0 AND late_deduction >= 0 AND undertime_deduction >= 0
             AND absence_deduction >= 0
             AND gross_pay >= 0 AND total_deductions >= 0 AND net_pay >= 0
            THEN 'PASS' ELSE 'FAIL' END,
       CASE WHEN basic_pay >= 0 AND overtime_pay >= 0
             AND rice_subsidy >= 0 AND phone_allowance >= 0 AND clothing_allowance >= 0
             AND sss_deduction >= 0 AND philhealth_deduction >= 0 AND pagibig_deduction >= 0
             AND withholding_tax >= 0 AND late_deduction >= 0 AND undertime_deduction >= 0
             AND absence_deduction >= 0
             AND gross_pay >= 0 AND total_deductions >= 0 AND net_pay >= 0
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-006  (Edge Case) Required header & total fields are non-null
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-006', 'Edge Case',
       'Required header & total fields are non-null',
       'PASS',
       CASE WHEN employee_name IS NOT NULL AND position_name IS NOT NULL
             AND department_name IS NOT NULL AND supervisor_name IS NOT NULL
             AND tin_number IS NOT NULL AND sss_number IS NOT NULL
             AND philhealth_number IS NOT NULL AND pagibig_number IS NOT NULL
             AND period_start IS NOT NULL AND period_end IS NOT NULL
             AND pay_date IS NOT NULL AND net_pay IS NOT NULL
            THEN 'PASS' ELSE 'FAIL' END,
       CASE WHEN employee_name IS NOT NULL AND position_name IS NOT NULL
             AND department_name IS NOT NULL AND supervisor_name IS NOT NULL
             AND tin_number IS NOT NULL AND sss_number IS NOT NULL
             AND philhealth_number IS NOT NULL AND pagibig_number IS NOT NULL
             AND period_start IS NOT NULL AND period_end IS NOT NULL
             AND pay_date IS NOT NULL AND net_pay IS NOT NULL
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-007  (Business Rule) Pay period ordering: start <= end <= pay_date
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-007', 'Business Rule',
       'period_start <= period_end <= pay_date',
       'PASS',
       CASE WHEN period_start <= period_end AND period_end <= pay_date
            THEN 'PASS' ELSE 'FAIL' END,
       CASE WHEN period_start <= period_end AND period_end <= pay_date
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_employee_payslip_report
WHERE employee_id = @test_emp AND pay_run_id = @test_run;

-- =============================================================================
-- DBT-008  (Negative) No orphan payroll_earning rows (FK integrity)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-008', 'Negative',
       'No orphan payroll_earning rows (FK integrity)',
       '0', a.cnt,
       CASE WHEN a.cnt = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM payroll_earning pe
       LEFT JOIN payroll_result r ON r.payroll_result_id = pe.payroll_result_id
      WHERE r.payroll_result_id IS NULL) a;

-- =============================================================================
-- DBT-009  (Negative) No orphan payroll_deduction rows (FK integrity)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-009', 'Negative',
       'No orphan payroll_deduction rows (FK integrity)',
       '0', a.cnt,
       CASE WHEN a.cnt = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM payroll_deduction pd
       LEFT JOIN payroll_result r ON r.payroll_result_id = pd.payroll_result_id
      WHERE r.payroll_result_id IS NULL) a;

-- =============================================================================
-- DBT-010  (Negative) No orphan payroll_benefit rows (FK integrity)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-010', 'Negative',
       'No orphan payroll_benefit rows (FK integrity)',
       '0', a.cnt,
       CASE WHEN a.cnt = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM payroll_benefit pb
       LEFT JOIN payroll_result r ON r.payroll_result_id = pb.payroll_result_id
      WHERE r.payroll_result_id IS NULL) a;

-- =============================================================================
-- DBT-011  (Business Rule) ALL payslips reconcile (whole-table identity check)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-011', 'Business Rule',
       'ALL payslips reconcile (deductions + net identity)',
       '0', a.mismatches,
       CASE WHEN a.mismatches = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT SUM(CASE WHEN ROUND(net_pay,2) <> ROUND(gross_pay - total_deductions,2)
                      OR ROUND(sss_deduction + philhealth_deduction + pagibig_deduction
                               + withholding_tax + late_deduction + undertime_deduction
                               + absence_deduction, 2) <> ROUND(total_deductions,2)
                    THEN 1 ELSE 0 END) AS mismatches
    FROM vw_employee_payslip_report
) a;

-- =============================================================================
-- DBT-012  (Edge Case) View row count equals payroll_result row count
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-012', 'Edge Case',
       'View row count equals payroll_result row count (no loss/dupe)',
       (SELECT COUNT(*) FROM payroll_result),
       (SELECT COUNT(*) FROM vw_employee_payslip_report),
       CASE WHEN (SELECT COUNT(*) FROM vw_employee_payslip_report)
                = (SELECT COUNT(*) FROM payroll_result)
            THEN 'PASS' ELSE 'FAIL' END;

-- NOTE: The withholding-tax exemption (TRAIN floor) is verified in
--   payroll_summary_report_tests.sql (test DBM-011), NOT here. Tax is computed
--   on the MONTHLY basis (full monthly basic minus monthly statutory contribs)
--   and applied only on the 2nd cutoff, so a per-payslip semi-monthly check
--   would compare the wrong granularity against the monthly bracket floor.

-- =============================================================================
-- DBT-013  (Edge Case) Em-dash placeholder when government IDs are missing
--   The view must never expose NULL government numbers; missing data shows '—'.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBT-013', 'Edge Case',
       'Missing government IDs render as em-dash, never NULL',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT SUM(CASE WHEN tin_number IS NULL OR sss_number IS NULL
                      OR philhealth_number IS NULL OR pagibig_number IS NULL
                    THEN 1 ELSE 0 END) AS nulls
    FROM vw_employee_payslip_report
) a;


-- =============================================================================
-- RESULTS  -- detailed grid, category breakdown, then the overall verdict
-- =============================================================================
SELECT test_id, category, scenario, expected, actual, result
FROM   test_results
ORDER BY test_id;

-- Per-category pass/fail counts.
SELECT category,
       COUNT(*)                                        AS tests,
       SUM(result = 'PASS')                            AS passed,
       SUM(result = 'FAIL')                            AS failed
FROM   test_results
GROUP BY category
ORDER BY category;

-- Single overall verdict: the suite PASSES only if every test passed.
SELECT 'SUITE SUMMARY'                                 AS report,
       COUNT(*)                                        AS total_tests,
       SUM(result = 'PASS')                            AS passed,
       SUM(result = 'FAIL')                            AS failed,
       CASE WHEN SUM(result = 'FAIL') = 0
            THEN 'ALL TESTS PASSED'
            ELSE CONCAT(SUM(result = 'FAIL'), ' TEST(S) FAILED') END AS verdict
FROM   test_results;
