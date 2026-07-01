-- =============================================================================
-- MotorPH Payroll System
-- Payroll Summary Report (Monthly) - View Definition and Report Output
-- DBMS: MySQL 8.x
-- File: database/Sql Scripts/payroll_summary_monthly_view.sql
-- -----------------------------------------------------------------------------
-- Purpose:
--   Creates the source view for the monthly Payroll Summary Report.
--
--   Since payroll is processed twice a month, this view combines the two cutoff
--   payslips into one monthly summary per employee.
--
--   The report output at the bottom shows the Payroll Month/Year column between
--   Department and Gross Income, and includes a GRAND TOTAL row for the amount
--   columns.
--
-- Output:
--   One row per employee per payroll month, with monthly gross income, benefits,
--   deductions, and net pay.
--
-- Report refinements:
--   * Payroll Month/Year is included as a readable label, such as June 2024.
--   * Payroll Month/Year is shown between Department and Gross Income.
--   * Amount columns are totaled using a GRAND TOTAL row.
--   * Monetary fields use COALESCE(..., 0) so totals do not return blank values.
--   * Only active employees are included in the active payroll summary report.
--
-- Employee status note:
--   Active employees are identified by is_active.
--   is_active = 1 means active.
--   is_active = 0 means inactive.
--
--   employment_status is the employment type, such as REGULAR, PROBATIONARY, or
--   CONTRACTUAL. It is not used to check whether an employee is active.
--
-- Design notes:
--   * Rows are grouped by the calendar month of the pay-period end date.
--   * Earnings, benefits, and deductions are summarized per payslip first, then
--     rolled up monthly.
--   * Monthly gross, deductions, and net pay are summed from payroll_result so
--     the report stays consistent with the saved payroll values.
--
-- Safety note:
--   This script recreates the report view and displays a read-only report output.
--   It does not insert, update, delete, or change payroll transaction records.
-- =============================================================================

USE payrollsystem_db;

DROP VIEW IF EXISTS vw_payroll_summary_monthly;

CREATE VIEW vw_payroll_summary_monthly AS
SELECT
    -- ---- Grouping keys ------------------------------------------------------
    res.employee_id,
    DATE_FORMAT(pr.end_date, '%Y-%m')                     AS payroll_month,

    -- Readable payroll month label for the report, such as June 2024.
    DATE_FORMAT(MAX(pr.end_date), '%M %Y')                AS payroll_month_year,

    MIN(pr.start_date)                                    AS month_start,
    MAX(pr.end_date)                                      AS month_end,

    -- ---- Employee / organization details -----------------------------------
    CONCAT(emp.first_name, ' ', emp.last_name)            AS employee_name,
    pos.position_name                                     AS position_name,
    dept.department_name                                  AS department_name,

    -- ---- Government ID numbers ---------------------------------------------
    COALESCE(gov.sss_number,        '—')                  AS sss_number,
    COALESCE(gov.philhealth_number, '—')                  AS philhealth_number,
    COALESCE(gov.pagibig_number,    '—')                  AS pagibig_number,
    COALESCE(gov.tin_number,        '—')                  AS tin_number,

    -- ---- Monthly payroll count and hours -----------------------------------
    COUNT(DISTINCT res.payroll_result_id)                 AS payslip_count,
    SUM(res.total_hours_worked)                           AS total_hours_worked,

    -- ---- Monthly earnings ---------------------------------------------------
    COALESCE(SUM(ern.basic_pay),    0)                    AS basic_pay,
    COALESCE(SUM(ern.overtime_pay), 0)                    AS overtime_pay,

    -- ---- Monthly benefits ---------------------------------------------------
    COALESCE(SUM(ben.rice_subsidy),       0)              AS rice_subsidy,
    COALESCE(SUM(ben.phone_allowance),    0)              AS phone_allowance,
    COALESCE(SUM(ben.clothing_allowance), 0)              AS clothing_allowance,
    COALESCE(SUM(ben.total_benefits),     0)              AS total_benefits,

    -- ---- Monthly deductions -------------------------------------------------
    COALESCE(SUM(ded.sss_deduction),        0)            AS sss_deduction,
    COALESCE(SUM(ded.philhealth_deduction), 0)            AS philhealth_deduction,
    COALESCE(SUM(ded.pagibig_deduction),    0)            AS pagibig_deduction,
    COALESCE(SUM(ded.withholding_tax),      0)            AS withholding_tax,
    COALESCE(SUM(ded.late_deduction),       0)            AS late_deduction,
    COALESCE(SUM(ded.undertime_deduction),  0)            AS undertime_deduction,
    COALESCE(SUM(ded.absence_deduction),    0)            AS absence_deduction,

    -- ---- Official monthly totals -------------------------------------------
    COALESCE(SUM(res.gross_pay),        0)                AS monthly_gross_pay,
    COALESCE(SUM(res.total_deductions), 0)                AS monthly_total_deductions,
    COALESCE(SUM(res.net_pay),          0)                AS monthly_net_pay

FROM payroll_result res
    JOIN payrun pr
        ON pr.pay_run_id = res.pay_run_id

    JOIN employee emp
        ON emp.employee_id = res.employee_id

    JOIN position pos
        ON pos.position_id = emp.position_id

    JOIN department dept
        ON dept.department_id = emp.department_id

    LEFT JOIN government_details gov
        ON gov.employee_id = emp.employee_id

    -- Earnings summarized to one row per payslip.
    LEFT JOIN (
        SELECT
            pe.payroll_result_id,
            SUM(CASE WHEN et.code = 'BASIC_PAY'
                     THEN pe.amount ELSE 0 END) AS basic_pay,
            SUM(CASE WHEN et.code = 'OVERTIME_PAY'
                     THEN pe.amount ELSE 0 END) AS overtime_pay
        FROM payroll_earning pe
        JOIN earning_type et
            ON et.earning_type_id = pe.earning_type_id
        GROUP BY pe.payroll_result_id
    ) ern
        ON ern.payroll_result_id = res.payroll_result_id

    -- Benefits summarized to one row per payslip.
    LEFT JOIN (
        SELECT
            pb.payroll_result_id,
            SUM(CASE WHEN bt.code = 'RICE_SUBSIDY'
                     THEN pb.amount ELSE 0 END) AS rice_subsidy,
            SUM(CASE WHEN bt.code = 'PHONE_ALLOWANCE'
                     THEN pb.amount ELSE 0 END) AS phone_allowance,
            SUM(CASE WHEN bt.code = 'CLOTHING_ALLOWANCE'
                     THEN pb.amount ELSE 0 END) AS clothing_allowance,
            SUM(pb.amount) AS total_benefits
        FROM payroll_benefit pb
        JOIN benefit_type bt
            ON bt.benefit_type_id = pb.benefit_type_id
        GROUP BY pb.payroll_result_id
    ) ben
        ON ben.payroll_result_id = res.payroll_result_id

    -- Deductions summarized to one row per payslip.
    LEFT JOIN (
        SELECT
            pd.payroll_result_id,
            SUM(CASE WHEN dt.code = 'SSS'
                     THEN pd.amount ELSE 0 END) AS sss_deduction,
            SUM(CASE WHEN dt.code = 'PHILHEALTH'
                     THEN pd.amount ELSE 0 END) AS philhealth_deduction,
            SUM(CASE WHEN dt.code = 'PAGIBIG'
                     THEN pd.amount ELSE 0 END) AS pagibig_deduction,
            SUM(CASE WHEN dt.code = 'WITHHOLDING_TAX'
                     THEN pd.amount ELSE 0 END) AS withholding_tax,
            SUM(CASE WHEN dt.code = 'LATE'
                     THEN pd.amount ELSE 0 END) AS late_deduction,
            SUM(CASE WHEN dt.code = 'UNDERTIME'
                     THEN pd.amount ELSE 0 END) AS undertime_deduction,
            SUM(CASE WHEN dt.code = 'ABSENCE'
                     THEN pd.amount ELSE 0 END) AS absence_deduction
        FROM payroll_deduction pd
        JOIN deduction_type dt
            ON dt.deduction_type_id = pd.deduction_type_id
        GROUP BY pd.payroll_result_id
    ) ded
        ON ded.payroll_result_id = res.payroll_result_id

-- Only active employees are included in the active payroll summary report.
WHERE emp.is_active = 1

GROUP BY
    res.employee_id,
    DATE_FORMAT(pr.end_date, '%Y-%m'),
    emp.first_name,
    emp.last_name,
    pos.position_name,
    dept.department_name,
    gov.sss_number,
    gov.philhealth_number,
    gov.pagibig_number,
    gov.tin_number;


-- =============================================================================
-- PAYROLL SUMMARY REPORT OUTPUT
-- -----------------------------------------------------------------------------
-- Shows the monthly Payroll Summary Report for the selected month.
-- Payroll Month/Year is placed between Department and Gross Income.
-- The GRAND TOTAL row shows totals for the amount columns while keeping the
-- government number columns in the report layout.
-- =============================================================================

SET @report_month := '2024-06';

SELECT
    `Employee No`,
    `Employee Full Name`,
    `Position`,
    `Department`,
    `Payroll Month/Year`,
    `Gross Income`,
    `Social Security No.`,
    `Social Security Contribution`,
    `Philhealth No.`,
    `Philhealth Contribution`,
    `Pag-ibig No.`,
    `Pag-Ibig Contribution`,
    `TIN`,
    `Withholding Tax`,
    `Net Pay`
FROM (
    -- Detail rows: one row per active employee for the selected month.
    SELECT
        0                                           AS sort_order,
        CAST(employee_id AS CHAR)                   AS `Employee No`,
        employee_name                               AS `Employee Full Name`,
        position_name                               AS `Position`,
        department_name                             AS `Department`,
        payroll_month_year                          AS `Payroll Month/Year`,
        monthly_gross_pay                           AS `Gross Income`,

        sss_number                                  AS `Social Security No.`,
        sss_deduction                               AS `Social Security Contribution`,

        philhealth_number                           AS `Philhealth No.`,
        philhealth_deduction                        AS `Philhealth Contribution`,

        pagibig_number                              AS `Pag-ibig No.`,
        pagibig_deduction                           AS `Pag-Ibig Contribution`,

        tin_number                                  AS `TIN`,
        withholding_tax                             AS `Withholding Tax`,

        monthly_net_pay                             AS `Net Pay`
    FROM vw_payroll_summary_monthly
    WHERE payroll_month = @report_month

    UNION ALL

    -- Grand total row: totals only the amount columns.
    SELECT
        1                                           AS sort_order,
        'GRAND TOTAL'                               AS `Employee No`,
        CONCAT(COUNT(*), ' employee(s)')            AS `Employee Full Name`,
        ''                                          AS `Position`,
        ''                                          AS `Department`,
        MAX(payroll_month_year)                     AS `Payroll Month/Year`,
        COALESCE(SUM(monthly_gross_pay), 0)         AS `Gross Income`,

        ''                                          AS `Social Security No.`,
        COALESCE(SUM(sss_deduction), 0)             AS `Social Security Contribution`,

        ''                                          AS `Philhealth No.`,
        COALESCE(SUM(philhealth_deduction), 0)      AS `Philhealth Contribution`,

        ''                                          AS `Pag-ibig No.`,
        COALESCE(SUM(pagibig_deduction), 0)         AS `Pag-Ibig Contribution`,

        ''                                          AS `TIN`,
        COALESCE(SUM(withholding_tax), 0)           AS `Withholding Tax`,

        COALESCE(SUM(monthly_net_pay), 0)           AS `Net Pay`
    FROM vw_payroll_summary_monthly
    WHERE payroll_month = @report_month
) report
ORDER BY sort_order, `Employee No`;