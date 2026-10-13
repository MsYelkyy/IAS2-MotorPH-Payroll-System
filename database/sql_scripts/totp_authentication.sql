-- Authenticator-app (TOTP, RFC 6238) login and password recovery.
-- Safe to run more than once. Run it as a MySQL account that can create tables and alter them,
-- not as the backend account, which has no DDL rights.
--
-- Existing database:   run this script, then totp_backend_grants.sql (the backend account has no rights on the new
--                      tables until you do). Commands are in README.md.
-- Fresh docker volume: docker-compose.yml runs it automatically, before the backend account is created.
--
-- The TOTP secret is stored AES-256-GCM encrypted. The key is never stored here; the backend reads it from
-- the MOTORPH_TOTP_ENCRYPTION_KEY environment variable.

USE payrollsystem_db;

-- One row per enrolled user. A row exists only after the user proved they hold the secret by entering a valid
-- code, so an unconfirmed secret can never turn TOTP on. An admin reset deletes the row.
CREATE TABLE IF NOT EXISTS user_totp_credential (
    user_totp_credential_id INT NOT NULL AUTO_INCREMENT,
    user_account_id         INT NOT NULL,
    encrypted_secret        VARBINARY(128) NOT NULL,
    secret_iv               VARBINARY(16) NOT NULL,
    totp_enabled            TINYINT(1) NOT NULL DEFAULT 0,
    enrolled_at             DATETIME NULL,
    last_used_time_step     BIGINT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_totp_credential_id),
    UNIQUE KEY uq_user_totp_credential_user (user_account_id),
    CONSTRAINT fk_user_totp_credential_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (user_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Single-use password-reset tokens issued after a successful recovery TOTP check. Only the SHA-256 of the token is
-- stored, so a copy of this table cannot be used to reset a password.
CREATE TABLE IF NOT EXISTS password_recovery_token (
    token_hash      CHAR(64) NOT NULL,
    user_account_id INT NOT NULL,
    expires_at      DATETIME NOT NULL,
    consumed        TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_hash),
    KEY idx_password_recovery_token_user (user_account_id),
    CONSTRAINT fk_password_recovery_token_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (user_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- login_challenge now carries the purpose of the challenge, the number of wrong codes entered against it, and the
-- encrypted secret generated for a first-time enrollment (it lives only as long as the challenge does).
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_challenge' AND COLUMN_NAME = 'purpose');
SET @ddl := IF(@has_col = 0,
    "ALTER TABLE login_challenge ADD COLUMN purpose ENUM('TEMPORARY_PASSWORD','TOTP_SETUP','TOTP_LOGIN','PASSWORD_RECOVERY') NOT NULL DEFAULT 'TEMPORARY_PASSWORD'",
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_challenge' AND COLUMN_NAME = 'attempt_count');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE login_challenge ADD COLUMN attempt_count INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_challenge' AND COLUMN_NAME = 'pending_secret_encrypted');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE login_challenge ADD COLUMN pending_secret_encrypted VARBINARY(128) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_challenge' AND COLUMN_NAME = 'pending_secret_iv');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE login_challenge ADD COLUMN pending_secret_iv VARBINARY(16) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- A password reset signs every session of the user out: tokens issued at or before this moment stop working.
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'sessions_revoked_at');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE user_account ADD COLUMN sessions_revoked_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Admin-only permission for resetting another user's authenticator enrolment.
INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'RESET_USER_TOTP', 'Security', 'RESET', 'user_totp_credential',
       'Allows user to reset another user''s authenticator enrollment.'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'RESET_USER_TOTP');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON p.permission_key = 'RESET_USER_TOTP'
WHERE r.role_name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id);
