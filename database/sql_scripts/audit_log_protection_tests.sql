-- Tests for audit_log_protection.sql (ALP-### tests).
-- Checks that user_access_logs allows INSERT and SELECT but blocks UPDATE and DELETE,
-- and that logged_at always comes from the database.
-- The test rows are rolled back, so no data is left behind.
-- Grant and time zone tests need the Docker setup. In CI they show SKIP.
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
    SET @alp_db_now = NOW(3);
    SET @alp_default_logged_at = NULL;
    SET @alp_fake_logged_at = NULL;

    START TRANSACTION;

    INSERT INTO user_access_logs (user_account_id, action, details)
    VALUES (NULL, 'ALP_TEST', 'original');
    SET @alp_insert_rows = ROW_COUNT();
    SET @alp_probe_id = LAST_INSERT_ID();

    SELECT logged_at INTO @alp_default_logged_at
    FROM user_access_logs WHERE log_id = @alp_probe_id;

    INSERT INTO user_access_logs (user_account_id, action, details, logged_at)
    VALUES (NULL, 'ALP_TEST', 'fake time', '2000-01-01 00:00:00.000');
    SET @alp_fake_id = LAST_INSERT_ID();

    SELECT logged_at INTO @alp_fake_logged_at
    FROM user_access_logs WHERE log_id = @alp_fake_id;

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
SELECT 'ALP-009', 'Cleanup', 'Test rows were rolled back',
       '0', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM user_access_logs WHERE log_id IN (@alp_probe_id, @alp_fake_id);

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

-- ALP-013 to ALP-017: logged_at comes from the database (Control 5.2)
INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-013', 'Setup', 'BEFORE INSERT trigger exists on user_access_logs',
       '1', COUNT(*), CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END
FROM information_schema.TRIGGERS
WHERE EVENT_OBJECT_SCHEMA = 'payrollsystem_db'
  AND EVENT_OBJECT_TABLE = 'user_access_logs'
  AND ACTION_TIMING = 'BEFORE'
  AND EVENT_MANIPULATION = 'INSERT';

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-014', 'Setup', 'logged_at is DATETIME(3) with default CURRENT_TIMESTAMP(3)',
       'datetime(3) CURRENT_TIMESTAMP(3)',
       CONCAT(COLUMN_TYPE, ' ', COALESCE(COLUMN_DEFAULT, 'NULL')),
       CASE WHEN COLUMN_TYPE = 'datetime(3)'
             AND COLUMN_DEFAULT = 'CURRENT_TIMESTAMP(3)'
             AND IS_NULLABLE = 'NO'
            THEN 'PASS' ELSE 'FAIL' END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'payrollsystem_db'
  AND TABLE_NAME = 'user_access_logs'
  AND COLUMN_NAME = 'logged_at';

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-015', 'Positive', 'INSERT without logged_at gets the database time',
       'within 5s of NOW(3)', COALESCE(CAST(@alp_default_logged_at AS CHAR), 'NULL'),
       CASE WHEN ABS(TIMESTAMPDIFF(SECOND, @alp_default_logged_at, @alp_db_now)) <= 5
            THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-016', 'Negative', 'Supplied logged_at 2000-01-01 is replaced by database time',
       'within 5s of NOW(3)', COALESCE(CAST(@alp_fake_logged_at AS CHAR), 'NULL'),
       CASE WHEN ABS(TIMESTAMPDIFF(SECOND, @alp_fake_logged_at, @alp_db_now)) <= 5
            THEN 'PASS' ELSE 'FAIL' END;

INSERT INTO test_results (test_id, category, scenario, expected, actual, result)
SELECT 'ALP-017', 'Setup', 'Database time zone is +08:00',
       '+08:00', @@global.time_zone,
       CASE WHEN @alp_app_accounts = 0 THEN 'SKIP'
            WHEN @@global.time_zone = '+08:00' THEN 'PASS' ELSE 'FAIL' END;

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
