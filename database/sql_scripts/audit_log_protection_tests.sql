-- Tests for audit_log_protection.sql (ALP-### tests).
-- Checks that user_access_logs allows INSERT and SELECT but blocks UPDATE and DELETE.
-- The test row is rolled back, so no data is left behind.
-- Grant tests need the backend account (Docker). In CI there is none, so they show SKIP.
-- Run as root.

USE payrollsystem_db;

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

-- ALP-001, ALP-002: triggers exist
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-001', 'Setup', 'BEFORE UPDATE trigger exists on user_access_logs',
       '1', COUNT(*), CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END
FROM information_schema.TRIGGERS
WHERE EVENT_OBJECT_SCHEMA = 'payrollsystem_db'
  AND EVENT_OBJECT_TABLE = 'user_access_logs'
  AND ACTION_TIMING = 'BEFORE'
  AND EVENT_MANIPULATION = 'UPDATE';

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-002', 'Setup', 'BEFORE DELETE trigger exists on user_access_logs',
       '1', COUNT(*), CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END
FROM information_schema.TRIGGERS
WHERE EVENT_OBJECT_SCHEMA = 'payrollsystem_db'
  AND EVENT_OBJECT_TABLE = 'user_access_logs'
  AND ACTION_TIMING = 'BEFORE'
  AND EVENT_MANIPULATION = 'DELETE';

-- Insert a test row, try to change and delete it, then roll back.
DROP PROCEDURE IF EXISTS alp_probe;

DELIMITER //
CREATE PROCEDURE alp_probe()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET @alp_insert_rows = 0;
    SET @alp_select_rows = 0;
    SET @alp_update_blocked = 0;
    SET @alp_delete_blocked = 0;
    SET @alp_details_after = NULL;
    SET @alp_rows_after_delete = 0;

    START TRANSACTION;

    INSERT INTO user_access_logs (user_account_id, action, details, logged_at)
    VALUES (NULL, 'ALP_TEST', 'original', NOW());
    SET @alp_insert_rows = ROW_COUNT();
    SET @alp_probe_id = LAST_INSERT_ID();

    SELECT COUNT(*) INTO @alp_select_rows
    FROM user_access_logs WHERE log_id = @alp_probe_id;

    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET @alp_update_blocked = 1;
        UPDATE user_access_logs SET details = 'changed' WHERE log_id = @alp_probe_id;
    END;

    SELECT details INTO @alp_details_after
    FROM user_access_logs WHERE log_id = @alp_probe_id;

    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET @alp_delete_blocked = 1;
        DELETE FROM user_access_logs WHERE log_id = @alp_probe_id;
    END;

    SELECT COUNT(*) INTO @alp_rows_after_delete
    FROM user_access_logs WHERE log_id = @alp_probe_id;

    ROLLBACK;
END //
DELIMITER ;

CALL alp_probe();
DROP PROCEDURE IF EXISTS alp_probe;

-- ALP-003 to ALP-008: results from the test row
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-003', 'Positive', 'INSERT into user_access_logs is allowed',
       '1', @alp_insert_rows, CASE WHEN @alp_insert_rows = 1 THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-004', 'Positive', 'SELECT from user_access_logs is allowed',
       '1', @alp_select_rows, CASE WHEN @alp_select_rows = 1 THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-005', 'Negative', 'UPDATE on user_access_logs is blocked',
       'blocked', IF(@alp_update_blocked = 1, 'blocked', 'allowed'),
       CASE WHEN @alp_update_blocked = 1 THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-006', 'Negative', 'Row is unchanged after the UPDATE attempt',
       'original', COALESCE(@alp_details_after, 'NULL'),
       CASE WHEN @alp_details_after = 'original' THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-007', 'Negative', 'DELETE on user_access_logs is blocked',
       'blocked', IF(@alp_delete_blocked = 1, 'blocked', 'allowed'),
       CASE WHEN @alp_delete_blocked = 1 THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-008', 'Negative', 'Row still exists after the DELETE attempt',
       '1', @alp_rows_after_delete, CASE WHEN @alp_rows_after_delete = 1 THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-009', 'Cleanup', 'Test row was rolled back',
       '0', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM user_access_logs WHERE log_id = @alp_probe_id;

-- ALP-010 to ALP-012: backend account grants (SKIP when there is no backend account)
SET @alp_app_accounts := (
    SELECT COUNT(*) FROM (
        SELECT User, Host FROM mysql.db
        WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
        UNION
        SELECT User, Host FROM mysql.tables_priv
        WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
    ) a
);

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-010', 'Grant', 'No account has UPDATE or DELETE on the whole database',
       '0', COUNT(*),
       CASE WHEN @alp_app_accounts = 0 THEN 'SKIP'
            WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM mysql.db
WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
  AND (Update_priv = 'Y' OR Delete_priv = 'Y');

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-011', 'Grant', 'No account has UPDATE or DELETE on user_access_logs',
       '0', COUNT(*),
       CASE WHEN @alp_app_accounts = 0 THEN 'SKIP'
            WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM mysql.tables_priv
WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
  AND Table_name = 'user_access_logs'
  AND (FIND_IN_SET('Update', Table_priv) > 0 OR FIND_IN_SET('Delete', Table_priv) > 0);

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-012', 'Grant', 'Backend account can still INSERT and SELECT on the database',
       '0 missing', CONCAT(COUNT(*), ' missing'),
       CASE WHEN @alp_app_accounts = 0 THEN 'SKIP'
            WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM (
    SELECT User, Host FROM mysql.db
    WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
    UNION
    SELECT User, Host FROM mysql.tables_priv
    WHERE Db IN ('payrollsystem_db', 'payrollsystem\\_db')
) a
WHERE NOT EXISTS (
    SELECT 1 FROM mysql.db d
    WHERE d.User = a.User AND d.Host = a.Host
      AND d.Db IN ('payrollsystem_db', 'payrollsystem\\_db')
      AND d.Insert_priv = 'Y' AND d.Select_priv = 'Y'
);

-- Results
SELECT test_id, category, scenario, expected, actual, result
FROM   test_results
ORDER BY test_id;

SELECT 'SUITE SUMMARY'                 AS report,
       COUNT(*)                        AS total_tests,
       SUM(result = 'PASS')            AS passed,
       SUM(result = 'FAIL')            AS failed,
       SUM(result = 'SKIP')            AS skipped,
       CASE WHEN SUM(result = 'FAIL') = 0
            THEN 'ALL TESTS PASSED'
            ELSE CONCAT(SUM(result = 'FAIL'), ' TEST(S) FAILED') END AS verdict
FROM   test_results;
