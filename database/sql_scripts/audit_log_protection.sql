-- Makes user_access_logs append-only. Rows can be added and read, but not changed or deleted.
-- Safe to run more than once.

USE payrollsystem_db;

DROP TRIGGER IF EXISTS trg_user_access_logs_block_update;
DROP TRIGGER IF EXISTS trg_user_access_logs_block_delete;

CREATE TRIGGER trg_user_access_logs_block_update
BEFORE UPDATE ON user_access_logs
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log records cannot be updated.';

CREATE TRIGGER trg_user_access_logs_block_delete
BEFORE DELETE ON user_access_logs
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log records cannot be deleted.';
