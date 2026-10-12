-- Control 4.1: Prevent duplicate payroll records

USE payrollsystem_db;

START TRANSACTION;

CREATE TEMPORARY TABLE duplicate_payroll_results (
    payroll_result_id INT PRIMARY KEY
);

-- Identify duplicate payroll results.
-- The oldest result is retained.
INSERT INTO duplicate_payroll_results (payroll_result_id)
SELECT duplicate_result.payroll_result_id
FROM payroll_result duplicate_result
JOIN (
    SELECT
        employee_id,
        pay_run_id,
        MIN(payroll_result_id) AS retained_result_id
    FROM payroll_result
    GROUP BY employee_id, pay_run_id
    HAVING COUNT(*) > 1
) duplicate_group
    ON duplicate_group.employee_id = duplicate_result.employee_id
    AND duplicate_group.pay_run_id = duplicate_result.pay_run_id
WHERE duplicate_result.payroll_result_id
      <> duplicate_group.retained_result_id;

-- Remove components belonging to duplicate results.
DELETE component
FROM payroll_benefit component
JOIN duplicate_payroll_results duplicate_result
    ON duplicate_result.payroll_result_id =
       component.payroll_result_id;

DELETE component
FROM payroll_deduction component
JOIN duplicate_payroll_results duplicate_result
    ON duplicate_result.payroll_result_id =
       component.payroll_result_id;

DELETE component
FROM payroll_earning component
JOIN duplicate_payroll_results duplicate_result
    ON duplicate_result.payroll_result_id =
       component.payroll_result_id;

DELETE component
FROM payroll_overtime component
JOIN duplicate_payroll_results duplicate_result
    ON duplicate_result.payroll_result_id =
       component.payroll_result_id;

-- Remove the duplicate payroll results.
DELETE payroll
FROM payroll_result payroll
JOIN duplicate_payroll_results duplicate_result
    ON duplicate_result.payroll_result_id =
       payroll.payroll_result_id;

DROP TEMPORARY TABLE duplicate_payroll_results;

COMMIT;

-- Add database-level duplicate prevention.
ALTER TABLE payroll_result
ADD CONSTRAINT uq_payroll_result_employee_payrun
UNIQUE (employee_id, pay_run_id);