-- Control 4.1: Prevent duplicate payroll records
-- This migration does not automatically delete existing records.

USE payrollsystem_db;

DROP PROCEDURE IF EXISTS apply_payroll_integrity_constraint;

DELIMITER $$

CREATE PROCEDURE apply_payroll_integrity_constraint()
BEGIN
    DECLARE duplicate_group_count INT DEFAULT 0;
    DECLARE constraint_exists INT DEFAULT 0;

    -- Check whether duplicate employee_id and pay_run_id pairs exist.
    SELECT COUNT(*)
    INTO duplicate_group_count
    FROM (
        SELECT employee_id, pay_run_id
        FROM payroll_result
        GROUP BY employee_id, pay_run_id
        HAVING COUNT(*) > 1
    ) AS duplicate_groups;

    -- Report duplicates and stop the migration.
    IF duplicate_group_count > 0 THEN

        SELECT
            employee_id,
            pay_run_id,
            COUNT(*) AS duplicate_count,
            GROUP_CONCAT(
                payroll_result_id
                ORDER BY payroll_result_id
            ) AS payroll_result_ids
        FROM payroll_result
        GROUP BY employee_id, pay_run_id
        HAVING COUNT(*) > 1;

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Duplicate payroll records found. Review them manually before applying the UNIQUE constraint.';

    ELSE

        -- Check whether the UNIQUE constraint is already installed.
        SELECT COUNT(*)
        INTO constraint_exists
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'payroll_result'
          AND CONSTRAINT_NAME =
              'uq_payroll_result_employee_payrun'
          AND CONSTRAINT_TYPE = 'UNIQUE';

        -- Add the constraint only when it does not exist.
        IF constraint_exists = 0 THEN

            ALTER TABLE payroll_result
            ADD CONSTRAINT uq_payroll_result_employee_payrun
            UNIQUE (employee_id, pay_run_id);

            SELECT
                'UNIQUE constraint successfully installed.'
                AS migration_result;

        ELSE

            SELECT
                'UNIQUE constraint is already installed. No changes were made.'
                AS migration_result;

        END IF;

    END IF;
END$$

DELIMITER ;

CALL apply_payroll_integrity_constraint();

DROP PROCEDURE IF EXISTS apply_payroll_integrity_constraint;