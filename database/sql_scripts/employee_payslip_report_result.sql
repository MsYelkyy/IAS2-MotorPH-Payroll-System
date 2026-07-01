-- =============================================================================
-- MotorPH Payroll System
-- Employee Payslip Report  --  result query
-- -----------------------------------------------------------------------------
-- Purpose
--   Presentation output for the Employee Payslip Report, shown for the June 2024
--   period (both cutoffs: June 1-15 and June 16-30). Columns are renamed to the
--   payslip labels for a clean report or screenshot.
--
-- Source
--   vw_employee_payslip_report
--
-- Design notes
--   Gross Income, Total Deductions, and Take Home Pay come from the stored
--   payroll_result, so they stay consistent with the saved payroll values.
--
-- Safety note
--   Read-only. Does not change any business data.
-- =============================================================================
SELECT
    -- ---- Payslip Header -----------------------------------------------------
    CONCAT(pay_run_id, '-', DATE_FORMAT(pay_date, '%Y-%m-%d')) AS `Payslip No`,
    employee_id                                           AS `Employee ID`,
    employee_name                                         AS `Employee Name`,
    CONCAT(position_name, ' / ', department_name)         AS `Employee Position/Department`,
    period_start                                          AS `Period Start Date`,
    period_end                                            AS `Period End Date`,

    -- ---- Earnings ----------------------------------------------------------
    monthly_salary                                        AS `Monthly Salary`,
    daily_rate                                            AS `Daily Rate`,
    days_worked                                           AS `Days Worked`,
    overtime_pay                                          AS `Overtime`,
    gross_pay                                             AS `Gross Income`,

    -- ---- Benefits ----------------------------------------------------------
    rice_subsidy                                          AS `Rice Subsidy`,
    phone_allowance                                       AS `Phone Allowance`,
    clothing_allowance                                    AS `Clothing Allowance`,
    total_benefits                                        AS `Total Benefits`,

    -- ---- Deductions --------------------------------------------------------
    sss_deduction                                         AS `Social Security System`,
    philhealth_deduction                                  AS `Philhealth`,
    pagibig_deduction                                     AS `Pag-Ibig`,
    withholding_tax                                       AS `Withholding Tax`,
    late_deduction                                        AS `Late`,
    undertime_deduction                                   AS `Undertime`,
    absence_deduction                                     AS `Absence`,
    other_deductions                                      AS `Other Deductions`,
    total_deductions                                      AS `Total Deductions`,

    -- ---- Summary -----------------------------------------------------------
    gross_pay                                             AS `Gross Income (Summary)`,
    total_benefits                                        AS `Benefits`,
    total_deductions                                      AS `Deductions`,
    net_pay                                               AS `Take Home Pay`

FROM vw_employee_payslip_report
WHERE period_start >= '2024-06-01'
  AND period_end   <= '2024-06-30'
ORDER BY employee_name, period_start;