-- =============================================================================
-- MotorPH Payroll System
-- Employee Payslip Report  --  view definition
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/employee_payslip_report_view.sql
-- -----------------------------------------------------------------------------
-- Purpose
--   Creates the source view for the Employee Payslip Report. Payroll data is
--   spread across many tables (payslip totals, plus separate rows for each
--   earning, benefit, and deduction, and reference data like employee, position,
--   department, and government IDs). This view brings it all together so a
--   payslip can be read from one place.
--
--   Output: one row per payslip (one employee, one pay run), with every payslip
--   field already laid out as a column -- header details, pay period, earnings,
--   benefits, each deduction, and the official gross, total deductions, and net
--   pay. Reports, tests, and screens simply read from the view and filter by
--   employee or pay period.
--
-- Design notes
--   * Earnings, benefits, and deductions are summarized in their own subqueries
--     first (one row per payslip each), then joined. This keeps the component
--     totals accurate and avoids inflated sums.
--   * Components are matched by their stable code (BASIC_PAY, SSS, ...), so the
--     view stays correct even if reference tables are reloaded.
--   * COALESCE(...,0) keeps the payslip complete: a component with no data shows
--     0.00 instead of NULL.
--   * Gross pay, total deductions, and net pay are read straight from the stored
--     payroll_result, so the view always matches the official saved figures. The
--     broken-out columns just show how those totals are made up.
--
-- Report refinements
--   Active employees only: inactive, resigned, or terminated employees are
--   excluded from the active payslip report using emp.is_active = 1 (see the
--   WHERE clause at the end of the view). is_active (1 = active, 0 = inactive)
--   is the correct flag; employment_status is the employment TYPE, not the
--   active/inactive state.
--   >>> Adjust here if your schema names the column differently (e.g.
--       employee_status, status): change the emp.is_active = 1 filter below. <<<
--
-- Safety note
--   Recreates the vw_employee_payslip_report view (DROP + CREATE). Read-only
--   against business data -- it does not change any payroll records.
-- =============================================================================

USE payrollsystem_db;

DROP VIEW IF EXISTS vw_employee_payslip_report;

CREATE VIEW vw_employee_payslip_report AS
SELECT
    -- ---- Identity / routing keys -------------------------------------------
    res.payroll_result_id,
    res.employee_id,
    res.pay_run_id,

    -- ---- Employee header ----------------------------------------------------
    CONCAT(emp.first_name, ' ', emp.last_name)            AS employee_name,
    pos.position_name                                     AS position_name,
    dept.department_name                                  AS department_name,
    CASE
        WHEN sup.employee_id IS NULL THEN '—'
        ELSE CONCAT(sup.first_name, ' ', sup.last_name)
    END                                                   AS supervisor_name,

    -- ---- Government numbers (default em dash when unavailable) --------------
    COALESCE(gov.tin_number,        '—')                  AS tin_number,
    COALESCE(gov.sss_number,        '—')                  AS sss_number,
    COALESCE(gov.philhealth_number, '—')                  AS philhealth_number,
    COALESCE(gov.pagibig_number,    '—')                  AS pagibig_number,

    -- ---- Pay period ---------------------------------------------------------
    pr.start_date                                         AS period_start,
    pr.end_date                                           AS period_end,
    pr.pay_date                                           AS pay_date,
    pr.payroll_type                                       AS payroll_type,
    res.total_hours_worked,

    -- ---- Salary reference (matches the payslip EARNINGS block) -------------
    --   Uses the salary in effect on the pay-period end date. Monthly salary is
    --   the full monthly basic; daily rate is the hourly rate x 8 (8-hour day).
    COALESCE(sh.basic_salary, 0)                          AS monthly_salary,
    ROUND(COALESCE(sh.hourly_rate, 0) * 8, 2)             AS daily_rate,

    -- ---- Days worked = hours worked / 8 (8-hour workday) ------------------
    ROUND(res.total_hours_worked / 8, 2)                  AS days_worked,

    -- ---- Earnings (pre-aggregated; one value per payslip) ------------------
    COALESCE(ern.basic_pay,    0)                         AS basic_pay,
    COALESCE(ern.overtime_pay, 0)                         AS overtime_pay,

    -- ---- Benefits / allowances (pre-aggregated) ----------------------------
    COALESCE(ben.rice_subsidy,       0)                   AS rice_subsidy,
    COALESCE(ben.phone_allowance,    0)                   AS phone_allowance,
    COALESCE(ben.clothing_allowance, 0)                   AS clothing_allowance,
    COALESCE(ben.total_benefits,     0)                   AS total_benefits,

    -- ---- Total gross compensation (earnings + benefits) --------------------
    (
        COALESCE(ern.basic_pay, 0)
      + COALESCE(ern.overtime_pay, 0)
      + COALESCE(ben.total_benefits, 0)
    )                                                     AS total_gross_compensation,

    -- ---- Deductions (pre-aggregated; broken out by stable code) ------------
    COALESCE(ded.sss_deduction,        0)                 AS sss_deduction,
    COALESCE(ded.philhealth_deduction, 0)                 AS philhealth_deduction,
    COALESCE(ded.pagibig_deduction,    0)                 AS pagibig_deduction,
    COALESCE(ded.withholding_tax,      0)                 AS withholding_tax,
    COALESCE(ded.late_deduction,       0)                 AS late_deduction,
    COALESCE(ded.undertime_deduction,  0)                 AS undertime_deduction,
    COALESCE(ded.absence_deduction,    0)                 AS absence_deduction,

    -- ---- Other deductions = attendance-based (late + undertime + absence) --
    --   Feeds the payslip "Other Deductions" line; the columns above keep the
    --   detail.
    COALESCE(ded.other_deductions, 0)                     AS other_deductions,

    -- ---- Official stored totals (single source of truth) -------------------
    res.gross_pay,
    res.total_deductions,
    res.net_pay,
    res.generated_at

FROM payroll_result res
    -- pay run / period
    JOIN payrun     pr   ON pr.pay_run_id   = res.pay_run_id
    -- employee + org structure
    JOIN employee   emp  ON emp.employee_id = res.employee_id
    JOIN position   pos  ON pos.position_id = emp.position_id
    JOIN department dept ON dept.department_id = emp.department_id
    LEFT JOIN employee sup ON sup.employee_id = emp.supervisor_id
    LEFT JOIN government_details gov ON gov.employee_id = emp.employee_id
    -- salary in effect on the pay-period end date
    LEFT JOIN salary_history sh
           ON sh.salary_history_id = (
                SELECT s.salary_history_id
                FROM   salary_history s
                WHERE  s.employee_id = res.employee_id
                  AND  s.start_date <= pr.end_date
                  AND  (s.end_date IS NULL OR s.end_date >= pr.end_date)
                ORDER BY s.start_date DESC
                LIMIT 1
           )
    -- Earnings summarized to one row per payslip (keeps totals accurate).
    LEFT JOIN (
        SELECT pe.payroll_result_id,
               SUM(CASE WHEN et.code = 'BASIC_PAY'    THEN pe.amount ELSE 0 END) AS basic_pay,
               SUM(CASE WHEN et.code = 'OVERTIME_PAY' THEN pe.amount ELSE 0 END) AS overtime_pay
        FROM   payroll_earning pe
        JOIN   earning_type    et ON et.earning_type_id = pe.earning_type_id
        GROUP BY pe.payroll_result_id
    ) ern ON ern.payroll_result_id = res.payroll_result_id
    -- Benefits summarized to one row per payslip.
    LEFT JOIN (
        SELECT pb.payroll_result_id,
               SUM(CASE WHEN bt.code = 'RICE_SUBSIDY'       THEN pb.amount ELSE 0 END) AS rice_subsidy,
               SUM(CASE WHEN bt.code = 'PHONE_ALLOWANCE'    THEN pb.amount ELSE 0 END) AS phone_allowance,
               SUM(CASE WHEN bt.code = 'CLOTHING_ALLOWANCE' THEN pb.amount ELSE 0 END) AS clothing_allowance,
               SUM(pb.amount)                                                          AS total_benefits
        FROM   payroll_benefit pb
        JOIN   benefit_type    bt ON bt.benefit_type_id = pb.benefit_type_id
        GROUP BY pb.payroll_result_id
    ) ben ON ben.payroll_result_id = res.payroll_result_id
    -- Deductions summarized to one row per payslip, broken out by type.
    LEFT JOIN (
        SELECT pd.payroll_result_id,
               SUM(CASE WHEN dt.code = 'SSS'             THEN pd.amount ELSE 0 END) AS sss_deduction,
               SUM(CASE WHEN dt.code = 'PHILHEALTH'      THEN pd.amount ELSE 0 END) AS philhealth_deduction,
               SUM(CASE WHEN dt.code = 'PAGIBIG'         THEN pd.amount ELSE 0 END) AS pagibig_deduction,
               SUM(CASE WHEN dt.code = 'WITHHOLDING_TAX' THEN pd.amount ELSE 0 END) AS withholding_tax,
               SUM(CASE WHEN dt.code = 'LATE'            THEN pd.amount ELSE 0 END) AS late_deduction,
               SUM(CASE WHEN dt.code = 'UNDERTIME'       THEN pd.amount ELSE 0 END) AS undertime_deduction,
               SUM(CASE WHEN dt.code = 'ABSENCE'         THEN pd.amount ELSE 0 END) AS absence_deduction,
               SUM(CASE WHEN dt.code IN ('LATE','UNDERTIME','ABSENCE')
                        THEN pd.amount ELSE 0 END)                                  AS other_deductions
        FROM   payroll_deduction pd
        JOIN   deduction_type    dt ON dt.deduction_type_id = pd.deduction_type_id
        GROUP BY pd.payroll_result_id
    ) ded ON ded.payroll_result_id = res.payroll_result_id

-- Only active employees are included in the active payslip report.
-- (Adjust the column name here if your schema differs -- see header note.)
WHERE emp.is_active = 1;

-- =============================================================================
-- USAGE 1 -- one employee's complete payslip for a single cutoff
--   (replace the ids with any seeded employee_id / pay_run_id pair)
-- =============================================================================
-- SELECT *
-- FROM   vw_employee_payslip_report
-- WHERE  employee_id = 10001
--   AND  pay_run_id  = 1;

-- =============================================================================
-- USAGE 2 -- Presentation output (payslip template labels)
--   Same data as the view, with columns renamed to the official payslip labels
--   so a report or screenshot reads like the printed payslip. Sections follow
--   the template order: header, earnings, benefits, deductions, summary. A
--   semi-monthly month returns two rows (both cutoffs). Edit the filter as
--   needed.
-- =============================================================================
SELECT
    -- ---- Header -------------------------------------------------------------
    CONCAT(pay_run_id, '-', DATE_FORMAT(pay_date, '%Y-%m-%d')) AS `Payslip No`,
    employee_id                                          AS `Employee ID`,
    employee_name                                        AS `Employee Name`,
    CONCAT(position_name, ' / ', department_name)        AS `Employee Position/Department`,
    period_start                                         AS `Period Start Date`,
    period_end                                           AS `Period End Date`,

    -- ---- Earnings -----------------------------------------------------------
    monthly_salary                                       AS `Monthly Salary`,
    daily_rate                                           AS `Daily Rate`,
    days_worked                                          AS `Days Worked`,
    overtime_pay                                         AS `Overtime`,
    -- Gross Income uses the official stored gross_pay, so it always matches the
    -- saved payroll figures.
    gross_pay                                            AS `Gross Income`,

    -- ---- Benefits -----------------------------------------------------------
    rice_subsidy                                         AS `Rice Subsidy`,
    phone_allowance                                      AS `Phone Allowance`,
    clothing_allowance                                   AS `Clothing Allowance`,
    total_benefits                                       AS `Total`,

    -- ---- Deductions ---------------------------------------------------------
    --   Statutory deductions (SSS, PhilHealth, Pag-IBIG, withholding tax) are
    --   computed monthly and applied on the 2nd cutoff, so the 1st cutoff often
    --   shows 0 for these. Attendance deductions (late, undertime, absence) are
    --   listed here and also rolled up as Other Deductions, so Total Deductions
    --   always reconciles on screen.
    sss_deduction                                        AS `Social Security System`,
    philhealth_deduction                                 AS `Philhealth`,
    pagibig_deduction                                    AS `Pag-Ibig`,
    withholding_tax                                      AS `Withholding Tax`,
    late_deduction                                       AS `Late`,
    undertime_deduction                                  AS `Undertime`,
    absence_deduction                                    AS `Absence`,
    other_deductions                                     AS `Other Deductions`,
    total_deductions                                     AS `Total Deductions`,

    -- ---- Summary (recap of the totals) --------------------------------------
    --   Restates gross income, benefits, deductions, and take-home pay. The
    --   "(Summary)" suffix keeps the column headers unique.
    gross_pay                                            AS `Gross Income (Summary)`,
    total_benefits                                       AS `Benefits`,
    total_deductions                                     AS `Deductions`,
    net_pay                                              AS `Take Home Pay`
FROM   vw_employee_payslip_report
WHERE  employee_id = 10001
  AND  period_start >= '2024-06-01'
  AND  period_end   <= '2024-06-30'
ORDER BY period_start;
