-- =============================================================================
-- MotorPH Payroll System
-- Payroll Summary Report (Monthly)  --  validation script
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/payroll_summary_report_tests.sql
-- -----------------------------------------------------------------------------
-- Validation purpose
--   Automated checks for the monthly Payroll Summary view
--   (vw_payroll_summary_monthly). Each check records a PASS or FAIL into a
--   temporary results table, and a final summary reports the total passed and
--   failed with one overall verdict.
--
--   The summary combines the two semi-monthly payslips of a month into one
--   monthly row, so these tests focus on:
--     * Correct roll-up (one row per employee per month)
--     * Monthly totals match the sum of the related payslips
--     * Monthly identity (net = gross - deductions)
--     * Government IDs shown (never blank)
--     * Totals not inflated by joins
--
--   Test IDs use the DBM-### convention (Database, Monthly), grouped by category:
--   Positive / Negative / Boundary / Edge Case / Business Rule / Consistency.
--
-- Safety note
--   Read-only against business data; writes only a temporary results table.
--
-- Prereqs (run in order):
--   1. SOURCE payrollsystem_db.sql;
--   2. SOURCE seed_payroll_earning.sql;
--   3. SOURCE employee_payslip_report_view.sql;   (payslip view, for cross-check)
--   4. SOURCE payroll_summary_monthly_view.sql;   (the view under test)
--   5. SOURCE payroll_summary_report_tests.sql;   (this file)
-- =============================================================================

USE payrollsystem_db;

-- -----------------------------------------------------------------------------
-- Test harness (temporary results table -- read-only against business data)
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
-- Test target: pick one (employee, calendar month) that actually has TWO
-- semi-monthly cutoffs, so the roll-up assertions are meaningful. Falls back to
-- whatever exists if no two-cutoff month is present.
-- -----------------------------------------------------------------------------
SET @test_emp := (
    SELECT res.employee_id
    FROM   payroll_result res
    JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
    GROUP BY res.employee_id, DATE_FORMAT(pr.end_date, '%Y-%m')
    ORDER BY COUNT(*) DESC, res.employee_id ASC
    LIMIT 1
);
SET @test_month := (
    SELECT DATE_FORMAT(pr.end_date, '%Y-%m')
    FROM   payroll_result res
    JOIN   payrun pr ON pr.pay_run_id = res.pay_run_id
    WHERE  res.employee_id = @test_emp
    GROUP BY DATE_FORMAT(pr.end_date, '%Y-%m')
    ORDER BY COUNT(*) DESC
    LIMIT 1
);

SELECT '--- Test target ---' AS info, @test_emp AS employee_id, @test_month AS payroll_month;


-- =============================================================================
-- DBM-001  (Positive) One row per (employee, month)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-001', 'Positive',
       'Summary returns exactly one row per employee per month',
       '1', a.cnt,
       CASE WHEN a.cnt = 1 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT COUNT(*) AS cnt FROM vw_payroll_summary_monthly
       WHERE employee_id = @test_emp AND payroll_month = @test_month) a;

-- =============================================================================
-- DBM-002  (Positive) payslip_count reflects the combined cutoffs
--   Must equal the number of payroll_result rows for that employee+month.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-002', 'Positive',
       'payslip_count equals number of cutoffs rolled up',
       (SELECT COUNT(*) FROM payroll_result res
          JOIN payrun pr ON pr.pay_run_id = res.pay_run_id
         WHERE res.employee_id = @test_emp
           AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month),
       (SELECT payslip_count FROM vw_payroll_summary_monthly
         WHERE employee_id = @test_emp AND payroll_month = @test_month),
       CASE WHEN (SELECT payslip_count FROM vw_payroll_summary_monthly
                   WHERE employee_id = @test_emp AND payroll_month = @test_month)
               = (SELECT COUNT(*) FROM payroll_result res
                    JOIN payrun pr ON pr.pay_run_id = res.pay_run_id
                   WHERE res.employee_id = @test_emp
                     AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month)
            THEN 'PASS' ELSE 'FAIL' END;

-- =============================================================================
-- DBM-003  (Business Rule) Monthly net = monthly gross - monthly deductions
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-003', 'Business Rule',
       'monthly_net_pay = monthly_gross_pay - monthly_total_deductions',
       ROUND(monthly_gross_pay - monthly_total_deductions, 2),
       ROUND(monthly_net_pay, 2),
       CASE WHEN ROUND(monthly_net_pay,2)
               = ROUND(monthly_gross_pay - monthly_total_deductions, 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_payroll_summary_monthly
WHERE employee_id = @test_emp AND payroll_month = @test_month;

-- =============================================================================
-- DBM-004  (Consistency) Monthly gross should match the sum of payslip gross
--   Confirms the monthly summary equals the total of that month's payslips.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-004', 'Consistency',
       'monthly_gross_pay equals SUM of per-payslip gross_pay',
       ROUND((SELECT SUM(p.gross_pay) FROM vw_employee_payslip_report p
                JOIN payrun pr ON pr.pay_run_id = p.pay_run_id
               WHERE p.employee_id = @test_emp
                 AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2),
       ROUND(monthly_gross_pay, 2),
       CASE WHEN ROUND(monthly_gross_pay,2) =
                 ROUND((SELECT SUM(p.gross_pay) FROM vw_employee_payslip_report p
                          JOIN payrun pr ON pr.pay_run_id = p.pay_run_id
                         WHERE p.employee_id = @test_emp
                           AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_payroll_summary_monthly
WHERE employee_id = @test_emp AND payroll_month = @test_month;

-- =============================================================================
-- DBM-005  (Consistency / CROSS-VIEW) Monthly net == SUM of payslip net
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-005', 'Consistency',
       'monthly_net_pay equals SUM of per-payslip net_pay',
       ROUND((SELECT SUM(p.net_pay) FROM vw_employee_payslip_report p
                JOIN payrun pr ON pr.pay_run_id = p.pay_run_id
               WHERE p.employee_id = @test_emp
                 AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2),
       ROUND(monthly_net_pay, 2),
       CASE WHEN ROUND(monthly_net_pay,2) =
                 ROUND((SELECT SUM(p.net_pay) FROM vw_employee_payslip_report p
                          JOIN payrun pr ON pr.pay_run_id = p.pay_run_id
                         WHERE p.employee_id = @test_emp
                           AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_payroll_summary_monthly
WHERE employee_id = @test_emp AND payroll_month = @test_month;

-- =============================================================================
-- DBM-006  (Business Rule) Component deductions reconcile to monthly total
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-006', 'Business Rule',
       'Monthly component deductions reconcile to monthly_total_deductions',
       ROUND(monthly_total_deductions, 2),
       ROUND(sss_deduction + philhealth_deduction + pagibig_deduction
             + withholding_tax + late_deduction + undertime_deduction
             + absence_deduction, 2),
       CASE WHEN ROUND(sss_deduction + philhealth_deduction + pagibig_deduction
                       + withholding_tax + late_deduction + undertime_deduction
                       + absence_deduction, 2) = ROUND(monthly_total_deductions, 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_payroll_summary_monthly
WHERE employee_id = @test_emp AND payroll_month = @test_month;

-- =============================================================================
-- DBM-007  (Negative) Benefits total is not inflated by the joins
--   total_benefits should equal the sum of the month's benefit amounts exactly.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-007', 'Negative',
       'total_benefits not inflated by JOIN fan-out',
       ROUND((SELECT COALESCE(SUM(pb.amount),0)
                FROM payroll_benefit pb
                JOIN payroll_result res ON res.payroll_result_id = pb.payroll_result_id
                JOIN payrun pr ON pr.pay_run_id = res.pay_run_id
               WHERE res.employee_id = @test_emp
                 AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2),
       ROUND(total_benefits, 2),
       CASE WHEN ROUND(total_benefits,2) =
                 ROUND((SELECT COALESCE(SUM(pb.amount),0)
                          FROM payroll_benefit pb
                          JOIN payroll_result res ON res.payroll_result_id = pb.payroll_result_id
                          JOIN payrun pr ON pr.pay_run_id = res.pay_run_id
                         WHERE res.employee_id = @test_emp
                           AND DATE_FORMAT(pr.end_date,'%Y-%m') = @test_month), 2)
            THEN 'PASS' ELSE 'FAIL' END
FROM vw_payroll_summary_monthly
WHERE employee_id = @test_emp AND payroll_month = @test_month;

-- =============================================================================
-- DBM-008  (Edge Case) Government ID enrichment present (em-dash, never NULL)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-008', 'Edge Case',
       'Government ID numbers are non-null (em-dash when missing)',
       '0', a.nulls,
       CASE WHEN a.nulls = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT SUM(CASE WHEN sss_number IS NULL OR philhealth_number IS NULL
                       OR pagibig_number IS NULL OR tin_number IS NULL
                      THEN 1 ELSE 0 END) AS nulls
      FROM vw_payroll_summary_monthly) a;

-- =============================================================================
-- DBM-009  (Boundary) All monthly monetary fields are non-negative (whole view)
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-009', 'Boundary',
       'All monthly monetary fields are non-negative (whole view)',
       '0', a.violations,
       CASE WHEN a.violations = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT SUM(CASE WHEN monthly_gross_pay < 0 OR monthly_total_deductions < 0
                       OR monthly_net_pay < 0 OR total_benefits < 0
                       OR sss_deduction < 0 OR philhealth_deduction < 0
                       OR pagibig_deduction < 0 OR withholding_tax < 0
                      THEN 1 ELSE 0 END) AS violations
      FROM vw_payroll_summary_monthly) a;

-- =============================================================================
-- DBM-010  (Consistency) Whole-view reconciliation across ALL months/employees
--   Every summary row must satisfy net = gross - deductions. Expected = 0.
-- =============================================================================
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-010', 'Consistency',
       'ALL summary rows satisfy net = gross - deductions',
       '0', a.mismatches,
       CASE WHEN a.mismatches = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (SELECT SUM(CASE WHEN ROUND(monthly_net_pay,2)
                        <> ROUND(monthly_gross_pay - monthly_total_deductions,2)
                      THEN 1 ELSE 0 END) AS mismatches
      FROM vw_payroll_summary_monthly) a;


-- =============================================================================
-- DBM-011  (Business Rule) Withholding tax exemption floor (TRAIN law)
--   Test: employees whose monthly taxable pay is at or below the 0%% tax bracket
--   should have zero withholding tax. Taxable pay = monthly basic minus the
--   mandatory contributions (SSS + PhilHealth + Pag-IBIG). Expected violations = 0.
--   This is checked here (monthly), since tax is applied on a monthly basis.
-- =============================================================================
SET @tax_floor := (
    SELECT max_salary FROM withholding_tax_bracket
    WHERE tax_rate = 0.00
      AND (effective_end_date IS NULL OR effective_end_date >= CURDATE())
    ORDER BY max_salary DESC LIMIT 1
);
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'DBM-011', 'Business Rule',
       'Below monthly tax floor => zero withholding tax (TRAIN exemption)',
       '0', a.violations,
       CASE WHEN a.violations = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT SUM(CASE
            WHEN ((monthly_gross_pay - total_benefits)
                  - (sss_deduction + philhealth_deduction + pagibig_deduction)) <= @tax_floor
                 AND withholding_tax > 0
            THEN 1 ELSE 0 END) AS violations
    FROM vw_payroll_summary_monthly
) a;


-- =============================================================================
-- RESULTS  -- detailed grid, category breakdown, then the overall verdict
-- =============================================================================
SELECT test_id, category, scenario, expected, actual, result
FROM   test_results
ORDER BY test_id;

SELECT category,
       COUNT(*)             AS tests,
       SUM(result = 'PASS') AS passed,
       SUM(result = 'FAIL') AS failed
FROM   test_results
GROUP BY category
ORDER BY category;

SELECT 'SUITE SUMMARY'                 AS report,
       COUNT(*)                        AS total_tests,
       SUM(result = 'PASS')            AS passed,
       SUM(result = 'FAIL')            AS failed,
       CASE WHEN SUM(result = 'FAIL') = 0
            THEN 'ALL TESTS PASSED'
            ELSE CONCAT(SUM(result = 'FAIL'), ' TEST(S) FAILED') END AS verdict
FROM   test_results;
