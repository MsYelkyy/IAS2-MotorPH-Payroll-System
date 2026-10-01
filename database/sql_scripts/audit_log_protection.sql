-- Makes user_access_logs append-only. Rows can be added and read, but not changed or deleted.
-- logged_at is always set by the database, even if the INSERT sends a value.
-- Safe to run more than once.

USE payrollsystem_db;

DROP TRIGGER IF EXISTS trg_user_access_logs_set_logged_at;
DROP TRIGGER IF EXISTS trg_user_access_logs_block_update;
DROP TRIGGER IF EXISTS trg_user_access_logs_block_delete;

CREATE TRIGGER trg_user_access_logs_set_logged_at
BEFORE INSERT ON user_access_logs
FOR EACH ROW
SET NEW.logged_at = CURRENT_TIMESTAMP(3);

CREATE TRIGGER trg_user_access_logs_block_update
BEFORE UPDATE ON user_access_logs
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log records cannot be updated.';

CREATE TRIGGER trg_user_access_logs_block_delete
BEFORE DELETE ON user_access_logs
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log records cannot be deleted.';
