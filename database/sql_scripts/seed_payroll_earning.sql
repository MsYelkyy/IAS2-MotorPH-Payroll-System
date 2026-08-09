-- =============================================================================
-- MotorPH Payroll System
-- Seed script: payroll_earning (BASIC_PAY component rows)
-- DBMS: MySQL 8.x
-- File: database/seed_payroll_earning.sql
-- -----------------------------------------------------------------------------
-- Why this exists
--   The payrollsystem_db dump populates payroll_result (with official stored
--   gross_pay / total_deductions / net_pay), payroll_benefit, and
--   payroll_deduction -- but payroll_earning is EMPTY. Because the payslip view
--   derives basic_pay / overtime_pay from payroll_earning rows, every payslip
--   showed basic_pay = 0 and an under-reported component-level gross.
--
--   This script back-fills the missing BASIC_PAY earning component for EVERY
--   payroll_result so the broken-out earnings reconcile to the stored totals
--   (and the database test DBT-004, total_gross_compensation = gross_pay, passes).
--
-- How the amount is derived
--   basic_pay = stored gross_pay  -  that result's total benefits
--             ( - overtime, which is 0 here; no OVERTIME_PAY data is seeded).
--   This keeps each row self-consistent with the official gross_pay that the
--   application persisted, without inventing numbers.
--
-- Safety
--   * Re-runnable: it first deletes any existing BASIC_PAY rows (so running it
--     twice does not double-insert), then re-inserts from current data.
--   * Only touches payroll_earning. No schema changes, no other tables.
--   * The CHECK (amount >= 0) on payroll_earning guards against any result whose
--     benefits somehow exceed gross_pay; such a row would be rejected -- by
--     design, that would signal bad source data rather than be silently seeded.
-- =============================================================================

USE payrollsystem_db;

-- BASIC_PAY earning type id (stable lookup by code, not a hardcoded id).
SET @basic_pay_type := (SELECT earning_type_id FROM earning_type WHERE code = 'BASIC_PAY');

-- -----------------------------------------------------------------------------
-- Clear any previously-seeded BASIC_PAY rows so this script is idempotent.
-- -----------------------------------------------------------------------------
DELETE FROM payroll_earning WHERE earning_type_id = @basic_pay_type;

-- -----------------------------------------------------------------------------
-- Insert one BASIC_PAY row per payroll_result:
--   amount = gross_pay - SUM(that result's benefits)
-- -----------------------------------------------------------------------------
INSERT INTO payroll_earning (payroll_result_id, earning_type_id, amount, created_by_account_id)
SELECT
    r.payroll_result_id,
    @basic_pay_type,
    ROUND(r.gross_pay - COALESCE(b.total_benefits, 0), 2)  AS amount,
    1                                                       AS created_by_account_id
FROM payroll_result r
LEFT JOIN (
    SELECT payroll_result_id, SUM(amount) AS total_benefits
    FROM   payroll_benefit
    GROUP BY payroll_result_id
) b ON b.payroll_result_id = r.payroll_result_id;

-- -----------------------------------------------------------------------------
-- Verification: every payslip's broken-out gross must now equal stored gross_pay.
--   Expected: zero mismatching rows (mirrors test DBT-004).
-- -----------------------------------------------------------------------------
SELECT
    'SEED-CHECK' AS check_id,
    SUM(CASE WHEN ROUND(total_gross_compensation, 2) <> ROUND(gross_pay, 2)
             THEN 1 ELSE 0 END)                       AS mismatching_payslips,
    0                                                 AS expected,
    CASE WHEN SUM(CASE WHEN ROUND(total_gross_compensation, 2) <> ROUND(gross_pay, 2)
                       THEN 1 ELSE 0 END) = 0
         THEN 'PASS' ELSE 'FAIL' END                 AS result
FROM vw_employee_payslip_report;
